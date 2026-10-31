import matplotlib
matplotlib.use('Agg')  # Use a non-interactive backend to suppress GUI-related warnings
from flask import Flask, request, jsonify, send_from_directory, render_template_string, redirect, url_for, flash, session
from flask_cors import CORS  # To enable CORS for frontend-backend communication
from flask_sqlalchemy import SQLAlchemy
from flask_login import LoginManager, UserMixin, login_user, login_required, logout_user, current_user
from werkzeug.security import generate_password_hash, check_password_hash
from datetime import datetime
import yfinance as yf
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from sklearn.linear_model import LinearRegression
import os
import logging
import json

app = Flask(__name__)
CORS(app, supports_credentials=True)  # Enable Cross-Origin Resource Sharing with credentials

# App configuration
app.config['SECRET_KEY'] = 'your-secret-key-change-in-production'
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///stockvision.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['PERMANENT_SESSION_LIFETIME'] = 1800  # 30 minutes
app.config['SESSION_COOKIE_HTTPONLY'] = True
app.config['SESSION_COOKIE_SAMESITE'] = 'Lax'

# Initialize extensions
db = SQLAlchemy(app)
login_manager = LoginManager()
login_manager.init_app(app)
login_manager.login_view = 'login'

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Database Models
class User(UserMixin, db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(120), nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    # Relationships
    portfolios = db.relationship('Portfolio', backref='user', lazy=True)
    transactions = db.relationship('Transaction', backref='user', lazy=True)
    
    def set_password(self, password):
        self.password_hash = generate_password_hash(password)
    
    def check_password(self, password):
        return check_password_hash(self.password_hash, password)

class Portfolio(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    total_value = db.Column(db.Float, default=100000.0)  # Starting with 1,00,000 virtual money
    cash_balance = db.Column(db.Float, default=100000.0)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

class Transaction(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    stock_symbol = db.Column(db.String(20), nullable=False)
    stock_name = db.Column(db.String(100), nullable=False)
    quantity = db.Column(db.Integer, nullable=False)
    price = db.Column(db.Float, nullable=False)
    transaction_type = db.Column(db.String(10), nullable=False)  # 'BUY' or 'SELL'
    timestamp = db.Column(db.DateTime, default=datetime.utcnow)
    total_amount = db.Column(db.Float, nullable=False)

class Holding(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False)
    stock_symbol = db.Column(db.String(20), nullable=False)
    stock_name = db.Column(db.String(100), nullable=False)
    quantity = db.Column(db.Integer, nullable=False)
    average_price = db.Column(db.Float, nullable=False)
    
    user = db.relationship('User', backref='holdings')

@login_manager.user_loader
def load_user(user_id):
    return User.query.get(int(user_id))

@app.before_request
def before_request():
    # Ensure session is permanent for authenticated users
    if current_user.is_authenticated:
        session.permanent = True

# Initialize database
with app.app_context():
    db.create_all()
    # Create a default portfolio for existing users if any
    users_without_portfolio = User.query.outerjoin(Portfolio).filter(Portfolio.id == None).all()
    for user in users_without_portfolio:
        portfolio = Portfolio(user_id=user.id)
        db.session.add(portfolio)
    db.session.commit()

# Authentication Routes
@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        data = request.get_json()
        username = data.get('username')
        email = data.get('email')
        password = data.get('password')
        
        # Validation
        if not username or not email or not password:
            return jsonify({'error': 'All fields are required'}), 400
        
        if User.query.filter_by(username=username).first():
            return jsonify({'error': 'Username already exists'}), 400
        
        if User.query.filter_by(email=email).first():
            return jsonify({'error': 'Email already exists'}), 400
        
        # Create new user
        user = User(username=username, email=email)
        user.set_password(password)
        db.session.add(user)
        db.session.commit()
        
        # Create portfolio for the user
        portfolio = Portfolio(user_id=user.id)
        db.session.add(portfolio)
        db.session.commit()
        
        return jsonify({'message': 'User registered successfully'}), 201
    
    # GET request - return registration form
    return render_template_string('''
    <!DOCTYPE html>
    <html>
    <head>
        <title>Register - StockVision</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
        <style>
            :root {
                --primary-color: #7c3aed;
                --primary-hover: #8b5cf6;
                --secondary-color: #a855f7;
                --accent-color: #c084fc;
                --success-color: #10b981;
                --error-color: #ef4444;
                --text-dark: #f8fafc;
                --text-light: #9ca3af;
                --bg-black: #000000;
                --bg-dark: #0f0f0f;
                --bg-darker: #1a1a1a;
                --border-light: rgba(124, 58, 237, 0.3);
                --glass-bg: rgba(0, 0, 0, 0.7);
                --purple-glow: rgba(124, 58, 237, 0.5);
                --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
                --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
            }
            
            * { margin: 0; padding: 0; box-sizing: border-box; }
            
            body { 
                font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                background: radial-gradient(ellipse at top, #1a0f2e 0%, #0a0a0a 50%, #000000 100%);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 2rem;
                position: relative;
                overflow: hidden;
            }
            
            body::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: radial-gradient(ellipse at center, rgba(124, 58, 237, 0.1) 0%, rgba(0, 0, 0, 0.8) 70%);
                z-index: -1;
            }
            
            .container { 
                max-width: 450px; 
                width: 100%;
                background: var(--glass-bg); 
                padding: 3rem;
                border-radius: 20px;
                box-shadow: 0 25px 50px -12px rgba(124, 58, 237, 0.3), inset 0 1px 0 rgba(124, 58, 237, 0.2);
                position: relative;
                overflow: hidden;
                border: 1px solid var(--border-light);
                backdrop-filter: blur(15px);
                z-index: 1;
            }
            
            .container::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                height: 4px;
                background: linear-gradient(90deg, var(--primary-color), var(--accent-color), var(--secondary-color));
                box-shadow: 0 0 15px var(--purple-glow);
            }
            
            h2 { 
                color: var(--text-dark);
                text-align: center;
                margin-bottom: 2rem;
                font-size: 2rem;
                font-weight: 700;
                background: linear-gradient(135deg, #ffffff 0%, var(--accent-color) 50%, var(--primary-color) 100%);
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
                background-clip: text;
                text-shadow: 0 0 20px var(--purple-glow);
                animation: purpleGlow 3s ease-in-out infinite alternate;
            }
            
            @keyframes purpleGlow {
                from {
                    filter: drop-shadow(0 0 20px rgba(124, 58, 237, 0.5));
                }
                to {
                    filter: drop-shadow(0 0 40px rgba(124, 58, 237, 0.8));
                }
            }
            
            .form-group { 
                margin-bottom: 1.5rem;
                position: relative;
            }
            
            label { 
                display: block;
                margin-bottom: 0.5rem;
                font-weight: 600;
                color: var(--text-dark);
                font-size: 0.9rem;
            }
            
            input { 
                width: 100%;
                padding: 1rem;
                border: 2px solid var(--border-light);
                border-radius: 12px;
                font-size: 1rem;
                transition: all 0.3s ease;
                background: var(--bg-darker);
                color: var(--text-dark);
            }
            
            input:focus {
                outline: none;
                border-color: var(--primary-color);
                box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2), 0 0 20px rgba(124, 58, 237, 0.1);
                background: var(--bg-black);
                transform: scale(1.02);
            }
            
            button { 
                width: 100%;
                background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
                color: white;
                padding: 1rem;
                border: none;
                border-radius: 12px;
                cursor: pointer;
                font-size: 1.1rem;
                font-weight: 600;
                transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                box-shadow: 0 25px 50px -12px rgba(124, 58, 237, 0.3);
                position: relative;
                overflow: hidden;
            }
            
            button::before {
                content: '';
                position: absolute;
                top: 0;
                left: -100%;
                width: 100%;
                height: 100%;
                background: linear-gradient(90deg, transparent, rgba(192, 132, 252, 0.4), transparent);
                transition: left 0.5s;
            }
            
            button:hover::before { left: 100%; }
            
            button:hover { 
                background: linear-gradient(135deg, var(--primary-hover) 0%, var(--accent-color) 100%);
                transform: translateY(-2px);
                box-shadow: 0 25px 50px -12px rgba(124, 58, 237, 0.6), 0 0 30px rgba(124, 58, 237, 0.3);
            }
            
            .error { 
                color: var(--error-color);
                background: #fef2f2;
                border: 1px solid #fecaca;
                border-radius: 8px;
                padding: 1rem;
                margin-top: 1rem;
                font-weight: 500;
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            
            .error::before { content: '⚠️'; }
            
            .success { 
                color: #065f46;
                background: #d1fae5;
                border: 1px solid #a7f3d0;
                border-radius: 8px;
                padding: 1rem;
                margin-top: 1rem;
                font-weight: 500;
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            
            .success::before { content: '✅'; }
            
            .link { 
                text-align: center;
                margin-top: 2rem;
                padding-top: 2rem;
                border-top: 1px solid var(--border-light);
            }
            
            .link a { 
                color: var(--primary-color);
                text-decoration: none;
                font-weight: 600;
                padding: 0.5rem 1rem;
                border-radius: 8px;
                transition: all 0.3s ease;
            }
            
            .link a:hover {
                background: rgba(124, 58, 237, 0.2);
                transform: translateY(-1px);
            }

            .emoji { font-size: 3rem; text-align: center; margin-bottom: 1rem; }
            
            @media (max-width: 480px) {
                .container { padding: 2rem 1.5rem; }
                h2 { font-size: 1.5rem; }
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="emoji">🚀</div>
            <h2>Join StockVision</h2>
            <p style="text-align: center; color: #64748b; margin-bottom: 2rem;">Start your trading journey with ₹1,00,000 virtual money!</p>
            <form id="registerForm">
                <div class="form-group">
                    <label for="username">👤 Username:</label>
                    <input type="text" id="username" name="username" required placeholder="Choose a unique username">
                </div>
                <div class="form-group">
                    <label for="email">📧 Email:</label>
                    <input type="email" id="email" name="email" required placeholder="your.email@example.com">
                </div>
                <div class="form-group">
                    <label for="password">🔐 Password:</label>
                    <input type="password" id="password" name="password" required placeholder="Create a strong password">
                </div>
                <button type="submit">🎯 Create Account</button>
            </form>
            <div id="message"></div>
            <div class="link">
                <a href="/login">Already have an account? Sign in here</a>
            </div>
        </div>
        
        <script>
            document.getElementById('registerForm').addEventListener('submit', function(e) {
                e.preventDefault();
                
                const formData = {
                    username: document.getElementById('username').value,
                    email: document.getElementById('email').value,
                    password: document.getElementById('password').value
                };
                
                fetch('/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(formData)
                })
                .then(response => response.json())
                .then(data => {
                    const messageDiv = document.getElementById('message');
                    if (data.error) {
                        messageDiv.innerHTML = '<div class="error">' + data.error + '</div>';
                    } else {
                        messageDiv.innerHTML = '<div class="success">' + data.message + ' <a href="/login">Login now</a></div>';
                        document.getElementById('registerForm').reset();
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    document.getElementById('message').innerHTML = '<div class="error">An error occurred. Please try again.</div>';
                });
            });
        </script>
    </body>
    </html>
    ''')

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        data = request.get_json()
        username = data.get('username')
        password = data.get('password')
        
        if not username or not password:
            return jsonify({'error': 'Username and password are required'}), 400
        
        user = User.query.filter_by(username=username).first()
        
        if user and user.check_password(password):
            login_user(user, remember=True)
            session.permanent = True
            return jsonify({'message': 'Login successful', 'redirect': '/portfolio'}), 200
        else:
            return jsonify({'error': 'Invalid username or password'}), 401
    
    # GET request - return login form
    return render_template_string('''
    <!DOCTYPE html>
    <html>
    <head>
        <title>Login - StockVision</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
        <style>
            :root {
                --primary-color: #7c3aed;
                --primary-hover: #8b5cf6;
                --secondary-color: #a855f7;
                --accent-color: #c084fc;
                --success-color: #10b981;
                --error-color: #ef4444;
                --text-dark: #f8fafc;
                --text-light: #9ca3af;
                --bg-black: #000000;
                --bg-dark: #0f0f0f;
                --bg-darker: #1a1a1a;
                --border-light: rgba(124, 58, 237, 0.3);
                --glass-bg: rgba(0, 0, 0, 0.7);
                --purple-glow: rgba(124, 58, 237, 0.5);
                --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
                --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
            }
            
            * { margin: 0; padding: 0; box-sizing: border-box; }
            
            body { 
                font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                background: radial-gradient(ellipse at top, #1a0f2e 0%, #0a0a0a 50%, #000000 100%);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 2rem;
                position: relative;
                overflow: hidden;
            }
            
            body::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: radial-gradient(ellipse at center, rgba(124, 58, 237, 0.1) 0%, rgba(0, 0, 0, 0.8) 70%);
                z-index: -1;
            }
            
            .container { 
                max-width: 450px; 
                width: 100%;
                background: var(--glass-bg); 
                padding: 3rem;
                border-radius: 20px;
                box-shadow: 0 25px 50px -12px rgba(124, 58, 237, 0.3), inset 0 1px 0 rgba(124, 58, 237, 0.2);
                position: relative;
                overflow: hidden;
                border: 1px solid var(--border-light);
                backdrop-filter: blur(15px);
                z-index: 1;
            }
            
            .container::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                height: 4px;
                background: linear-gradient(90deg, var(--primary-color), var(--accent-color), var(--secondary-color));
                box-shadow: 0 0 15px var(--purple-glow);
            }
            
            h2 { 
                color: var(--text-dark);
                text-align: center;
                margin-bottom: 2rem;
                font-size: 2rem;
                font-weight: 700;
                background: linear-gradient(135deg, #ffffff 0%, var(--accent-color) 50%, var(--primary-color) 100%);
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
                background-clip: text;
                text-shadow: 0 0 20px var(--purple-glow);
                animation: purpleGlow 3s ease-in-out infinite alternate;
            }
            
            @keyframes purpleGlow {
                from {
                    filter: drop-shadow(0 0 20px rgba(124, 58, 237, 0.5));
                }
                to {
                    filter: drop-shadow(0 0 40px rgba(124, 58, 237, 0.8));
                }
            }
            
            .form-group { 
                margin-bottom: 1.5rem;
                position: relative;
            }
            
            label { 
                display: block;
                margin-bottom: 0.5rem;
                font-weight: 600;
                color: var(--text-dark);
                font-size: 0.9rem;
            }
            
            input { 
                width: 100%;
                padding: 1rem;
                border: 2px solid var(--border-light);
                border-radius: 12px;
                font-size: 1rem;
                transition: all 0.3s ease;
                background: var(--bg-darker);
                color: var(--text-dark);
            }
            
            input:focus {
                outline: none;
                border-color: var(--primary-color);
                box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.2), 0 0 20px rgba(124, 58, 237, 0.1);
                background: var(--bg-black);
                transform: scale(1.02);
            }
            
            button { 
                width: 100%;
                background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
                color: white;
                padding: 1rem;
                border: none;
                border-radius: 12px;
                cursor: pointer;
                font-size: 1.1rem;
                font-weight: 600;
                transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                box-shadow: 0 25px 50px -12px rgba(124, 58, 237, 0.3);
                position: relative;
                overflow: hidden;
            }
            
            button::before {
                content: '';
                position: absolute;
                top: 0;
                left: -100%;
                width: 100%;
                height: 100%;
                background: linear-gradient(90deg, transparent, rgba(192, 132, 252, 0.4), transparent);
                transition: left 0.5s;
            }
            
            button:hover::before { left: 100%; }
            
            button:hover { 
                background: linear-gradient(135deg, var(--primary-hover) 0%, var(--accent-color) 100%);
                transform: translateY(-2px);
                box-shadow: 0 25px 50px -12px rgba(124, 58, 237, 0.6), 0 0 30px rgba(124, 58, 237, 0.3);
            }
            
            .error { 
                color: var(--error-color);
                background: #fef2f2;
                border: 1px solid #fecaca;
                border-radius: 8px;
                padding: 1rem;
                margin-top: 1rem;
                font-weight: 500;
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            
            .error::before { content: '⚠️'; }
            
            .success { 
                color: #065f46;
                background: #d1fae5;
                border: 1px solid #a7f3d0;
                border-radius: 8px;
                padding: 1rem;
                margin-top: 1rem;
                font-weight: 500;
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            
            .success::before { content: '✅'; }
            
            .link { 
                text-align: center;
                margin-top: 2rem;
                padding-top: 2rem;
                border-top: 1px solid var(--border-light);
            }
            
            .link a { 
                color: var(--primary-color);
                text-decoration: none;
                font-weight: 600;
                padding: 0.5rem 1rem;
                border-radius: 8px;
                transition: all 0.3s ease;
            }
            
            .link a:hover {
                background: rgba(124, 58, 237, 0.2);
                transform: translateY(-1px);
            }

            .emoji { font-size: 3rem; text-align: center; margin-bottom: 1rem; }
            
            @media (max-width: 480px) {
                .container { padding: 2rem 1.5rem; }
                h2 { font-size: 1.5rem; }
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="emoji">🔐</div>
            <h2>Welcome Back</h2>
            <p style="text-align: center; color: #64748b; margin-bottom: 2rem;">Sign in to continue your trading journey</p>
            <form id="loginForm">
                <div class="form-group">
                    <label for="username">👤 Username:</label>
                    <input type="text" id="username" name="username" required placeholder="Enter your username">
                </div>
                <div class="form-group">
                    <label for="password">🔐 Password:</label>
                    <input type="password" id="password" name="password" required placeholder="Enter your password">
                </div>
                <button type="submit">🚀 Sign In</button>
            </form>
            <div id="message"></div>
            <div class="link">
                <a href="/register">Create New Account</a>
                <span style="color: #64748b;">|</span>
                <a href="/">Continue as Guest</a>
            </div>
        </div>
        
        <script>
            document.getElementById('loginForm').addEventListener('submit', function(e) {
                e.preventDefault();
                
                const formData = {
                    username: document.getElementById('username').value,
                    password: document.getElementById('password').value
                };
                
                fetch('/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(formData)
                })
                .then(response => response.json())
                .then(data => {
                    const messageDiv = document.getElementById('message');
                    if (data.error) {
                        messageDiv.innerHTML = '<div class="error">' + data.error + '</div>';
                    } else {
                        messageDiv.innerHTML = '<div class="success">' + data.message + ' Redirecting...</div>';
                        // Immediate redirect for better UX
                        if (data.success && data.redirect) {
                            window.location.href = data.redirect;
                        } else if (data.redirect) {
                            setTimeout(() => {
                                window.location.href = data.redirect;
                            }, 500);
                        }
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    document.getElementById('message').innerHTML = '<div class="error">An error occurred. Please try again.</div>';
                });
            });
        </script>
    </body>
    </html>
    ''')

@app.route('/logout')
@login_required
def logout():
    username = current_user.username
    logout_user()
    flash(f'Successfully logged out. See you again, {username}!', 'success')
    return redirect(url_for('serve_index2'))

# Test redirect route

# Authentication status endpoint
@app.route('/auth_status')
def auth_status():
    if current_user.is_authenticated:
        # Refresh session
        session.permanent = True
        return jsonify({
            'authenticated': True,
            'username': current_user.username,
            'user_id': current_user.id
        })
    else:
        return jsonify({'authenticated': False})

# Portfolio data endpoint
@app.route('/portfolio_data')
def portfolio_data():
    if not current_user.is_authenticated:
        return jsonify({'error': 'Authentication required'}), 401
    user_portfolio = Portfolio.query.filter_by(user_id=current_user.id).first()
    if user_portfolio:
        return jsonify({
            'cash_balance': user_portfolio.cash_balance,
            'total_value': user_portfolio.total_value
        })
    else:
        return jsonify({'error': 'Portfolio not found'}), 404

# Stock holdings endpoint
@app.route('/stock_holdings')
def stock_holdings():
    if not current_user.is_authenticated:
        return jsonify({'error': 'Authentication required'}), 401
    stock_name = request.args.get('stock')
    if not stock_name:
        return jsonify({'error': 'Stock parameter is required'}), 400
    
    symbols = {
        "Nestle": "NESTLEIND.NS", 
        "MarutiSuzuki": "MARUTI.NS", 
        "AxisBank": "AXISBANK.NS",
        "TCS": "TCS.NS",
        "Infosys": "INFY.NS",
        "HDFC": "HDFCBANK.NS",
        "ICICI": "ICICIBANK.NS",
        "SBI": "SBIN.NS",
        "Reliance": "RELIANCE.NS",
        "Wipro": "WIPRO.NS",
        "HUL": "HINDUNILVR.NS",
        "ITC": "ITC.NS",
        "Bajaj": "BAJFINANCE.NS",
        "Kotak": "KOTAKBANK.NS",
        "LT": "LT.NS"
    }
    if stock_name not in symbols:
        return jsonify({'error': 'Invalid stock selected'}), 400
    
    stock_symbol = symbols[stock_name]
    holding = Holding.query.filter_by(user_id=current_user.id, stock_symbol=stock_symbol).first()
    
    if holding:
        return jsonify({
            'holding': {
                'stock_name': holding.stock_name,
                'quantity': holding.quantity,
                'average_price': holding.average_price,
                'stock_symbol': holding.stock_symbol
            }
        })
    else:
        return jsonify({'holding': None})

# Trading history endpoint
@app.route('/trading_history')
def trading_history():
    if not current_user.is_authenticated:
        return jsonify({'error': 'Authentication required'}), 401
    stock_name = request.args.get('stock')
    if not stock_name:
        return jsonify({'error': 'Stock parameter is required'}), 400
    
    symbols = {
        "Nestle": "NESTLEIND.NS", 
        "MarutiSuzuki": "MARUTI.NS", 
        "AxisBank": "AXISBANK.NS",
        "TCS": "TCS.NS",
        "Infosys": "INFY.NS",
        "HDFC": "HDFCBANK.NS",
        "ICICI": "ICICIBANK.NS",
        "SBI": "SBIN.NS",
        "Reliance": "RELIANCE.NS",
        "Wipro": "WIPRO.NS",
        "HUL": "HINDUNILVR.NS",
        "ITC": "ITC.NS",
        "Bajaj": "BAJFINANCE.NS",
        "Kotak": "KOTAKBANK.NS",
        "LT": "LT.NS"
    }
    if stock_name not in symbols:
        return jsonify({'error': 'Invalid stock selected'}), 400
    
    stock_symbol = symbols[stock_name]
    transactions = Transaction.query.filter_by(
        user_id=current_user.id, 
        stock_symbol=stock_symbol
    ).order_by(Transaction.timestamp.desc()).limit(50).all()
    
    transaction_list = []
    for transaction in transactions:
        transaction_list.append({
            'stock_name': transaction.stock_name,
            'quantity': transaction.quantity,
            'price': transaction.price,
            'transaction_type': transaction.transaction_type,
            'timestamp': transaction.timestamp.isoformat(),
            'total_amount': transaction.total_amount
        })
    
    return jsonify({'transactions': transaction_list})

# Portfolio performance endpoint
@app.route('/portfolio_performance')
@login_required
def portfolio_performance():
    user_portfolio = Portfolio.query.filter_by(user_id=current_user.id).first()
    if not user_portfolio:
        return jsonify({'error': 'Portfolio not found'}), 404
    
    # Get all user's holdings
    holdings = Holding.query.filter_by(user_id=current_user.id).all()
    
    total_invested = 0
    current_value = user_portfolio.cash_balance
    holdings_data = []
    
    symbols = {
        "Nestle": "NESTLEIND.NS", 
        "MarutiSuzuki": "MARUTI.NS", 
        "AxisBank": "AXISBANK.NS",
        "TCS": "TCS.NS",
        "Infosys": "INFY.NS",
        "HDFC": "HDFCBANK.NS",
        "ICICI": "ICICIBANK.NS",
        "SBI": "SBIN.NS",
        "Reliance": "RELIANCE.NS",
        "Wipro": "WIPRO.NS",
        "HUL": "HINDUNILVR.NS",
        "ITC": "ITC.NS",
        "Bajaj": "BAJFINANCE.NS",
        "Kotak": "KOTAKBANK.NS",
        "LT": "LT.NS"
    }
    
    for holding in holdings:
        invested = holding.quantity * holding.average_price
        total_invested += invested
        
        # Try to get current price
        current_price = holding.average_price  # fallback
        try:
            if holding.stock_name in symbols:
                stock_data = fetch_stock_data(symbols[holding.stock_name], period="1d")
                if not stock_data.empty:
                    current_price = float(stock_data['Close'].iloc[-1])
        except:
            pass
        
        current_stock_value = holding.quantity * current_price
        current_value += current_stock_value
        
        pnl = current_stock_value - invested
        pnl_percentage = (pnl / invested) * 100 if invested > 0 else 0
        
        holdings_data.append({
            'stock_name': holding.stock_name,
            'quantity': holding.quantity,
            'average_price': holding.average_price,
            'current_price': current_price,
            'invested': invested,
            'current_value': current_stock_value,
            'pnl': pnl,
            'pnl_percentage': pnl_percentage
        })
    
    total_pnl = current_value - user_portfolio.total_value
    total_pnl_percentage = (total_pnl / user_portfolio.total_value) * 100 if user_portfolio.total_value > 0 else 0
    
    return jsonify({
        'cash_balance': user_portfolio.cash_balance,
        'total_invested': total_invested,
        'current_value': current_value,
        'total_pnl': total_pnl,
        'total_pnl_percentage': total_pnl_percentage,
        'holdings': holdings_data
    })

# Portfolio Management Routes
@app.route('/portfolio')
@login_required
def portfolio():
    try:
        user_portfolio = Portfolio.query.filter_by(user_id=current_user.id).first()
        if not user_portfolio:
            # Create portfolio if it doesn't exist
            user_portfolio = Portfolio(user_id=current_user.id)
            db.session.add(user_portfolio)
            db.session.commit()
            
        user_holdings = Holding.query.filter_by(user_id=current_user.id).all()
        recent_transactions = Transaction.query.filter_by(user_id=current_user.id).order_by(Transaction.timestamp.desc()).limit(10).all()
        
        # Calculate total portfolio value using the performance endpoint logic
        total_invested = 0
        current_value = user_portfolio.cash_balance
        holdings_with_current_prices = []
        
        symbols = {
        "Nestle": "NESTLEIND.NS", 
        "MarutiSuzuki": "MARUTI.NS", 
        "AxisBank": "AXISBANK.NS",
        "TCS": "TCS.NS",
        "Infosys": "INFY.NS",
        "HDFC": "HDFCBANK.NS",
        "ICICI": "ICICIBANK.NS",
        "SBI": "SBIN.NS",
        "Reliance": "RELIANCE.NS",
        "Wipro": "WIPRO.NS",
        "HUL": "HINDUNILVR.NS",
        "ITC": "ITC.NS",
        "Bajaj": "BAJFINANCE.NS",
        "Kotak": "KOTAKBANK.NS",
        "LT": "LT.NS"
    }
        
        for holding in user_holdings:
            invested = holding.quantity * holding.average_price
            total_invested += invested
            
            # Get current price
            current_price = holding.average_price  # fallback
            try:
                if holding.stock_name in symbols:
                    current_data = fetch_stock_data(symbols[holding.stock_name], period="1d")
                    if not current_data.empty:
                        current_price = float(current_data['Close'].iloc[-1])
            except:
                pass
            
            current_stock_value = holding.quantity * current_price
            current_value += current_stock_value
            
            pnl = current_stock_value - invested
            pnl_percentage = (pnl / invested) * 100 if invested > 0 else 0
            
            holdings_with_current_prices.append({
                'holding': holding,
                'current_price': current_price,
                'invested': invested,
                'current_value': current_stock_value,
                'pnl': pnl,
                'pnl_percentage': pnl_percentage
            })
        
        total_pnl = current_value - user_portfolio.total_value
        total_pnl_percentage = (total_pnl / user_portfolio.total_value) * 100 if user_portfolio.total_value > 0 else 0
    
    except Exception as e:
        logger.error(f"Error in portfolio route: {e}")
        return f"<h1>Error loading portfolio</h1><p>{str(e)}</p><a href='/'>Go Home</a>", 500
    
    return render_template_string('''
    <!DOCTYPE html>
    <html>
    <head>
        <title>Portfolio - StockVision</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
        <style>
            :root {
                --primary-color: #7c3aed;
                --primary-hover: #8b5cf6;
                --secondary-color: #a855f7;
                --accent-color: #c084fc;
                --success-color: #10b981;
                --warning-color: #f59e0b;
                --error-color: #ef4444;
                --text-dark: #f8fafc;
                --text-light: #9ca3af;
                --bg-black: #000000;
                --bg-dark: #0f0f0f;
                --bg-darker: #1a1a1a;
                --bg-white: #1a1a1a;
                --border-light: rgba(124, 58, 237, 0.3);
                --glass-bg: rgba(0, 0, 0, 0.7);
                --purple-glow: rgba(124, 58, 237, 0.5);
                --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
                --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
                --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
            }
            
            * { margin: 0; padding: 0; box-sizing: border-box; }
            
            body { 
                font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                background: radial-gradient(ellipse at top, #1a0f2e 0%, #0a0a0a 50%, #000000 100%);
                color: var(--text-dark);
                line-height: 1.6;
                min-height: 100vh;
            }
            
            .header { 
                background: linear-gradient(135deg, var(--primary-color) 0%, #1a0f2e 50%, var(--bg-black) 100%);
                color: white;
                padding: 2rem;
                position: relative;
                overflow: hidden;
            }
            
            .header::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="50" cy="50" r="1" fill="%23c084fc" opacity="0.3"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>') repeat;
                opacity: 0.5;
            }
            
            .header h1 {
                font-size: 2.5rem;
                font-weight: 700;
                margin: 0 0 1rem 0;
                position: relative;
                z-index: 1;
                background: linear-gradient(135deg, #ffffff 0%, var(--accent-color) 50%, var(--primary-color) 100%);
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
                background-clip: text;
                text-shadow: 0 0 20px var(--purple-glow);
                animation: purpleGlow 3s ease-in-out infinite alternate;
            }
            
            @keyframes purpleGlow {
                from {
                    filter: drop-shadow(0 0 20px rgba(124, 58, 237, 0.5));
                }
                to {
                    filter: drop-shadow(0 0 40px rgba(124, 58, 237, 0.8));
                }
            }
            
            .header p {
                position: relative;
                z-index: 1;
                color: var(--text-light);
                font-size: 1.1rem;
            }
            
            .header h1 { 
                margin: 0;
                font-size: 2.5rem;
                font-weight: 700;
                text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
                position: relative;
                z-index: 1;
            }
            
            .logout-btn { 
                position: absolute;
                top: 2rem;
                right: 2rem;
                background: rgba(239, 68, 68, 0.9);
                color: white;
                padding: 0.75rem 1.5rem;
                text-decoration: none;
                border-radius: 25px;
                font-weight: 600;
                transition: all 0.3s ease;
                backdrop-filter: blur(10px);
                z-index: 2;
                border: 1px solid rgba(239, 68, 68, 0.3);
            }
            
            .logout-btn:hover {
                background: #dc2626;
                transform: translateY(-2px);
                box-shadow: 0 25px 50px -12px rgba(239, 68, 68, 0.4);
                text-decoration: none;
                color: white;
            }
            
            .container { 
                max-width: 1400px;
                margin: 0 auto;
                padding: 3rem 2rem;
            }
            
            .card { 
                background: var(--bg-white);
                padding: 2rem;
                margin: 2rem 0;
                border-radius: 20px;
                box-shadow: 0 25px 50px -12px rgba(124, 58, 237, 0.3);
                border: 1px solid var(--border-light);
                transition: all 0.3s ease;
                position: relative;
                overflow: hidden;
            }
            
            .card::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                height: 4px;
                background: linear-gradient(90deg, var(--primary-color), var(--accent-color), var(--secondary-color));
                box-shadow: 0 0 10px var(--purple-glow);
            }
            
            .card:hover {
                transform: translateY(-5px);
                box-shadow: 0 35px 70px -12px rgba(124, 58, 237, 0.4), 0 0 50px rgba(124, 58, 237, 0.2);
                border-color: var(--primary-color);
            }
            
            h2 {
                color: var(--text-dark);
                margin-bottom: 2rem;
                font-size: 1.75rem;
                font-weight: 600;
                display: flex;
                align-items: center;
                gap: 0.5rem;
            }
            
            .stats { 
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                gap: 2rem;
                margin-bottom: 2rem;
            }
            
            .stat-item { 
                text-align: center;
                padding: 1.5rem;
                background: var(--bg-darker);
                border-radius: 16px;
                border: 1px solid var(--border-light);
                transition: all 0.3s ease;
            }
            
            .stat-item:hover {
                transform: scale(1.02);
                box-shadow: 0 10px 25px -5px rgba(124, 58, 237, 0.3);
                border-color: var(--primary-color);
            }
            
            .stat-label {
                font-size: 0.9rem;
                color: var(--text-light);
                font-weight: 500;
                margin-bottom: 0.5rem;
            }
            
            .stat-value { 
                font-size: 2rem;
                font-weight: 700;
                margin: 0.5rem 0;
                color: var(--accent-color);
            }
            
            .stat-change {
                font-size: 0.9rem;
                font-weight: 600;
            }
            
            .positive { color: var(--success-color); }
            .negative { color: var(--error-color); }
            
            .responsive-table {
                overflow-x: auto;
                border-radius: 12px;
                border: 1px solid var(--border-light);
                background: var(--bg-darker);
            }
            
            table { 
                width: 100%;
                border-collapse: collapse;
                font-size: 0.9rem;
            }
            
            th, td { 
                padding: 1rem 0.75rem;
                text-align: left;
                border-bottom: 1px solid rgba(124, 58, 237, 0.1);
                color: var(--text-dark);
            }
            
            th { 
                background: var(--bg-black);
                font-weight: 600;
                color: var(--text-dark);
                position: sticky;
                top: 0;
                z-index: 1;
            }
            
            tr:hover {
                background: rgba(124, 58, 237, 0.1);
            }
            
            .btn { 
                display: inline-flex;
                align-items: center;
                gap: 0.5rem;
                padding: 0.75rem 1.5rem;
                background: linear-gradient(135deg, var(--primary-color) 0%, var(--secondary-color) 100%);
                color: white;
                text-decoration: none;
                border-radius: 12px;
                font-weight: 600;
                font-size: 0.9rem;
                transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                box-shadow: 0 25px 50px -12px rgba(124, 58, 237, 0.3);
                border: none;
                cursor: pointer;
                margin: 0.5rem 0.5rem 0.5rem 0;
            }
            
            .btn:hover {
                background: linear-gradient(135deg, var(--primary-hover) 0%, var(--accent-color) 100%);
                transform: translateY(-2px);
                box-shadow: 0 25px 50px -12px rgba(124, 58, 237, 0.6), 0 0 30px rgba(124, 58, 237, 0.3);
                text-decoration: none;
                color: white;
            }
            
            .trading-buttons {
                display: flex;
                flex-wrap: wrap;
                gap: 1rem;
                margin-top: 1rem;
            }
            
            .trading-buttons .btn {
                margin: 0;
                flex: 0 0 auto;
            }
            
            .empty-state {
                text-align: center;
                padding: 3rem;
                color: var(--text-light);
            }
            
            .empty-state .emoji {
                font-size: 3rem;
                margin-bottom: 1rem;
            }
            
            @media (max-width: 768px) {
                .container { padding: 2rem 1rem; }
                .card { padding: 1.5rem; }
                .header h1 { font-size: 2rem; }
                .stats { grid-template-columns: 1fr; gap: 1rem; }
                .logout-btn {
                    position: relative;
                    top: auto;
                    right: auto;
                    margin-top: 1rem;
                    display: inline-block;
                }
                .trading-buttons {
                    gap: 0.75rem;
                }
                .trading-buttons .btn {
                    flex: 1 1 calc(50% - 0.375rem);
                    min-width: 0;
                    font-size: 0.8rem;
                    padding: 0.6rem 1rem;
                }
            }
            
            @media (max-width: 480px) {
                .trading-buttons .btn {
                    flex: 1 1 100%;
                    text-align: center;
                }
            }
        </style>
    </head>
    <body>
        <div class="header">
            <h1>📊 Welcome, {{ current_user.username }}!</h1>
            <a href="/logout" class="logout-btn">🚪 Logout</a>
        </div>
        
        <div class="container">
            <div class="card">
                <h2>📈 Portfolio Overview</h2>
                <div class="stats">
                    <div class="stat-item">
                        <div class="stat-label">Total Portfolio Value</div>
                        <div class="stat-value">₹{{ "%.2f"|format(current_value) }}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">Available Cash</div>
                        <div class="stat-value">₹{{ "%.2f"|format(user_portfolio.cash_balance) }}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">Total Invested</div>
                        <div class="stat-value">₹{{ "%.2f"|format(total_invested) }}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">Profit & Loss</div>
                        <div class="stat-value {{ 'positive' if total_pnl >= 0 else 'negative' }}">₹{{ "%.2f"|format(total_pnl) }}</div>
                        <div class="stat-change {{ 'positive' if total_pnl_percentage >= 0 else 'negative' }}">({{ "%.2f"|format(total_pnl_percentage) }}%)</div>
                    </div>
                </div>
            </div>
            
            <div class="card">
                <h2>📉 Your Stock Holdings</h2>
                {% if holdings_with_current_prices %}
                <div class="responsive-table">
                    <table>
                        <thead>
                            <tr>
                                <th>🏢 Stock</th>
                                <th>📈 Quantity</th>
                                <th>💰 Avg Price</th>
                                <th>💵 Current Price</th>
                                <th>📉 Invested</th>
                                <th>💸 Current Value</th>
                                <th>📈 P&L</th>
                            </tr>
                        </thead>
                        <tbody>
                            {% for item in holdings_with_current_prices %}
                            <tr>
                                <td><strong>{{ item.holding.stock_name }}</strong></td>
                                <td>{{ item.holding.quantity }}</td>
                                <td>₹{{ "%.2f"|format(item.holding.average_price) }}</td>
                                <td>₹{{ "%.2f"|format(item.current_price) }}</td>
                                <td>₹{{ "%.2f"|format(item.invested) }}</td>
                                <td>₹{{ "%.2f"|format(item.current_value) }}</td>
                                <td class="{{ 'positive' if item.pnl >= 0 else 'negative' }}">
                                    ₹{{ "%.2f"|format(item.pnl) }} ({{ "%.2f"|format(item.pnl_percentage) }}%)
                                </td>
                            </tr>
                            {% endfor %}
                        </tbody>
                    </table>
                </div>
                {% else %}
                <div class="empty-state">
                    <div class="emoji">📈</div>
                    <h3>No holdings yet</h3>
                    <p>Start trading to build your investment portfolio!</p>
                </div>
                {% endif %}
            </div>
            
            <div class="card">
                <h2>📃 Recent Transactions</h2>
                {% if recent_transactions %}
                <div class="responsive-table">
                    <table>
                        <thead>
                            <tr>
                                <th>📅 Date</th>
                                <th>🏢 Stock</th>
                                <th>🔄 Type</th>
                                <th>📈 Quantity</th>
                                <th>💰 Price</th>
                                <th>💸 Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            {% for transaction in recent_transactions %}
                            <tr>
                                <td>{{ transaction.timestamp.strftime('%Y-%m-%d %H:%M') }}</td>
                                <td><strong>{{ transaction.stock_name }}</strong></td>
                                <td class="{{ 'positive' if transaction.transaction_type == 'BUY' else 'negative' }}">{{ transaction.transaction_type }}</td>
                                <td>{{ transaction.quantity }}</td>
                                <td>₹{{ "%.2f"|format(transaction.price) }}</td>
                                <td>₹{{ "%.2f"|format(transaction.total_amount) }}</td>
                            </tr>
                            {% endfor %}
                        </tbody>
                    </table>
                </div>
                {% else %}
                <div class="empty-state">
                    <div class="emoji">📄</div>
                    <h3>No transactions yet</h3>
                    <p>Your trading history will appear here once you start trading.</p>
                </div>
                {% endif %}
            </div>
            
            <div class="card">
                <h2>🚀 Start Trading</h2>
                <p style="margin-bottom: 2rem; color: var(--text-light);">Explore stocks and make informed investment decisions with AI-powered predictions.</p>
                <div class="trading-buttons">
                    <a href="/" class="btn">📊 Stock Analysis Hub</a>
                    <a href="/stock/Nestle" class="btn">🥛 Trade Nestle</a>
                    <a href="/stock/MarutiSuzuki" class="btn">🚗 Trade Maruti Suzuki</a>
                    <a href="/stock/AxisBank" class="btn">🏦 Trade Axis Bank</a>
                    <a href="/stock/TCS" class="btn">💻 Trade TCS</a>
                    <a href="/stock/Infosys" class="btn">🌐 Trade Infosys</a>
                    <a href="/stock/HDFC" class="btn">🏛️ Trade HDFC Bank</a>
                    <a href="/stock/ICICI" class="btn">🏢 Trade ICICI Bank</a>
                    <a href="/stock/SBI" class="btn">🏦 Trade SBI</a>
                    <a href="/stock/Reliance" class="btn">⛽ Trade Reliance</a>
                    <a href="/stock/Wipro" class="btn">💼 Trade Wipro</a>
                    <a href="/stock/HUL" class="btn">🧴 Trade HUL</a>
                    <a href="/stock/ITC" class="btn">🚬 Trade ITC</a>
                    <a href="/stock/Bajaj" class="btn">💳 Trade Bajaj Finance</a>
                    <a href="/stock/Kotak" class="btn">🏛️ Trade Kotak Bank</a>
                    <a href="/stock/LT" class="btn">🏗️ Trade L&T</a>
                </div>
            </div>
        </div>
    </body>
    </html>
    ''', 
        current_user=current_user, 
        user_portfolio=user_portfolio, 
        holdings_with_current_prices=holdings_with_current_prices, 
        recent_transactions=recent_transactions,
        current_value=current_value,
        total_invested=total_invested,
        total_pnl=total_pnl,
        total_pnl_percentage=total_pnl_percentage
    )

# Trading functionality
@app.route('/trade', methods=['POST'])
def trade():
    if not current_user.is_authenticated:
        return jsonify({'error': 'Please login to start trading with virtual money!'}), 401
    
    data = request.get_json()
    stock_name = data.get('stock_name')
    quantity = int(data.get('quantity', 0))
    action = data.get('action')  # 'BUY' or 'SELL'
    
    if not stock_name or quantity <= 0 or action not in ['BUY', 'SELL']:
        return jsonify({'error': 'Invalid trade parameters'}), 400
    
    # Get current stock price
    symbols = {
        "Nestle": "NESTLEIND.NS", 
        "MarutiSuzuki": "MARUTI.NS", 
        "AxisBank": "AXISBANK.NS",
        "TCS": "TCS.NS",
        "Infosys": "INFY.NS",
        "HDFC": "HDFCBANK.NS",
        "ICICI": "ICICIBANK.NS",
        "SBI": "SBIN.NS",
        "Reliance": "RELIANCE.NS",
        "Wipro": "WIPRO.NS",
        "HUL": "HINDUNILVR.NS",
        "ITC": "ITC.NS",
        "Bajaj": "BAJFINANCE.NS",
        "Kotak": "KOTAKBANK.NS",
        "LT": "LT.NS"
    }
    if stock_name not in symbols:
        return jsonify({'error': 'Invalid stock selected'}), 400
    
    try:
        stock_data = fetch_stock_data(symbols[stock_name], period="1d")
        if stock_data.empty:
            return jsonify({'error': 'Could not fetch current stock price'}), 500
        
        current_price = float(stock_data['Close'].iloc[-1])
        total_amount = quantity * current_price
        
        user_portfolio = Portfolio.query.filter_by(user_id=current_user.id).first()
        
        if action == 'BUY':
            # Check if user has enough cash
            if user_portfolio.cash_balance < total_amount:
                return jsonify({'error': 'Insufficient cash balance'}), 400
            
            # Deduct cash
            user_portfolio.cash_balance -= total_amount
            
            # Update or create holding
            holding = Holding.query.filter_by(user_id=current_user.id, stock_symbol=symbols[stock_name]).first()
            if holding:
                # Update average price
                total_quantity = holding.quantity + quantity
                total_value = (holding.quantity * holding.average_price) + total_amount
                holding.average_price = total_value / total_quantity
                holding.quantity = total_quantity
            else:
                # Create new holding
                holding = Holding(
                    user_id=current_user.id,
                    stock_symbol=symbols[stock_name],
                    stock_name=stock_name,
                    quantity=quantity,
                    average_price=current_price
                )
                db.session.add(holding)
            
        elif action == 'SELL':
            # Check if user has enough stocks
            holding = Holding.query.filter_by(user_id=current_user.id, stock_symbol=symbols[stock_name]).first()
            if not holding or holding.quantity < quantity:
                return jsonify({'error': 'Insufficient stock quantity'}), 400
            
            # Add cash
            user_portfolio.cash_balance += total_amount
            
            # Update holding
            holding.quantity -= quantity
            if holding.quantity == 0:
                db.session.delete(holding)
        
        # Create transaction record
        transaction = Transaction(
            user_id=current_user.id,
            stock_symbol=symbols[stock_name],
            stock_name=stock_name,
            quantity=quantity,
            price=current_price,
            transaction_type=action,
            total_amount=total_amount
        )
        db.session.add(transaction)
        
        # Update portfolio timestamp
        user_portfolio.updated_at = datetime.utcnow()
        
        db.session.commit()
        
        return jsonify({
            'message': f'{action} order executed successfully',
            'transaction': {
                'stock': stock_name,
                'quantity': quantity,
                'price': current_price,
                'total': total_amount,
                'type': action
            },
            'new_cash_balance': user_portfolio.cash_balance
        }), 200
        
    except Exception as e:
        db.session.rollback()
        logger.error(f"Trading error: {e}")
        return jsonify({'error': 'An error occurred during trading'}), 500


# Function to fetch stock data
def fetch_stock_data(stock_symbol, period="2d"):
    try:
        logger.info(f"Fetching data for {stock_symbol} with period {period}")
        data = yf.download(stock_symbol, period=period, interval="1m", auto_adjust=True)
        if data.empty:
            logger.warning(f"No data returned for {stock_symbol}")
            return pd.DataFrame()
        return data
    except Exception as e:
        logger.error(f"Error fetching stock data: {e}")
        return pd.DataFrame()

# Function to predict stock prices
def predict_prices(data):
    try:
        data = data.reset_index()
        data['Timestamp'] = data['Datetime'].apply(lambda x: x.timestamp())
        X = np.array(data['Timestamp']).reshape(-1, 1)
        y = np.array(data['Close']).reshape(-1, 1)

        model = LinearRegression().fit(X, y)
        future_times = [X[-1][0] + 60, X[-1][0] + 3600, X[-1][0] + 86400]  # Next minute, hour, day
        predictions = model.predict(np.array(future_times).reshape(-1, 1))

        # Ensure predictions are not negative
        predictions = np.maximum(predictions, 0)
        
        return predictions.flatten()
    except Exception as e:
        logger.error(f"Error in prediction: {e}")
        return [0, 0, 0]

# Endpoint to fetch data and generate predictions
@app.route('/get_stock_data', methods=['GET'])
def get_stock_data():
    stock = request.args.get("stock")
    if not stock or stock not in ["Nestle", "MarutiSuzuki", "AxisBank", "TCS", "Infosys", "HDFC", "ICICI", "SBI", "Reliance", "Wipro", "HUL", "ITC", "Bajaj", "Kotak", "LT"]:
        logger.warning(f"Invalid stock selected: {stock}")
        return jsonify({"error": "Invalid stock selected. Please choose from our supported stock list."}), 400

    # Log request for debugging
    auth_status = {
        'authenticated': current_user.is_authenticated,
        'username': current_user.username if current_user.is_authenticated else None,
        'user_id': current_user.id if current_user.is_authenticated else None
    }
    logger.info(f"Get stock data request - Stock: {stock}, Auth Status: {auth_status}")

    symbols = {
        "Nestle": "NESTLEIND.NS", 
        "MarutiSuzuki": "MARUTI.NS", 
        "AxisBank": "AXISBANK.NS",
        "TCS": "TCS.NS",
        "Infosys": "INFY.NS",
        "HDFC": "HDFCBANK.NS",
        "ICICI": "ICICIBANK.NS",
        "SBI": "SBIN.NS",
        "Reliance": "RELIANCE.NS",
        "Wipro": "WIPRO.NS",
        "HUL": "HINDUNILVR.NS",
        "ITC": "ITC.NS",
        "Bajaj": "BAJFINANCE.NS",
        "Kotak": "KOTAKBANK.NS",
        "LT": "LT.NS"
    }
    stock_symbol = symbols[stock]

    try:
        data = fetch_stock_data(stock_symbol)
        if data.empty:
            return jsonify({"error": "Could not fetch data for the selected stock. This might be due to market hours or API limitations."}), 404

        predictions = predict_prices(data)

        # Plot historical data and predictions
        plt.figure(figsize=(10, 5))
        data['Close'].plot(label="Historical Data")
        future_index = [data.index[-1] + pd.Timedelta(seconds=60),
                        data.index[-1] + pd.Timedelta(hours=1),
                        data.index[-1] + pd.Timedelta(days=1)]
        plt.scatter(future_index, predictions, color='red', label="Predictions")
        plt.title(f"Stock Prices for {stock}")
        plt.xlabel("Time")
        plt.ylabel("Price")
        plt.legend()
        plt.grid(True, alpha=0.3)

        graph_path = f"static/graph_{stock.lower()}.png"
        plt.savefig(graph_path)
        plt.close()
        
        return jsonify({
            "stock": stock,
            "last_price": float(data['Close'].iloc[-1]),
            "predictions": {
                "next_minute": float(predictions[0]),
                "next_hour": float(predictions[1]),
                "next_day": float(predictions[2])
            },
            "graph_url": f"/{graph_path}"
        })
    except Exception as e:
        logger.error(f"Error processing request: {e}")
        return jsonify({"error": f"An error occurred: {str(e)}"}), 500

# Serve index2.html (Homepage) with authentication context
@app.route('/')
def serve_index2():
    # Pass authentication context to template
    auth_data = {
        'authenticated': current_user.is_authenticated,
        'username': current_user.username if current_user.is_authenticated else None,
        'user_id': current_user.id if current_user.is_authenticated else None
    }
    
    try:
        with open('index2.html', 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Inject authentication data directly into the JavaScript
        injection_script = f'''
        <script>
        // Injected authentication data
        window.authData = {json.dumps(auth_data)};
        </script>
        '''
        
        # Insert the script before the closing </head> tag
        content = content.replace('</head>', injection_script + '</head>')
        
        return content
    except FileNotFoundError:
        return send_from_directory('.', 'index2.html')

# Test route for debugging stock access
@app.route('/test_stock/<stock_name>')
def test_stock(stock_name):
    valid_stocks = ["Nestle", "MarutiSuzuki", "AxisBank", "TCS", "Infosys", "HDFC", "ICICI", "SBI", "Reliance", "Wipro", "HUL", "ITC", "Bajaj", "Kotak", "LT"]
    return f"Stock: {stock_name}, Valid: {stock_name in valid_stocks}"

# Serve stock analysis page with proper Flask template rendering
@app.route('/stock/<stock_name>')
def serve_stock_analysis(stock_name):
    # Validate stock name
    valid_stocks = ["Nestle", "MarutiSuzuki", "AxisBank", "TCS", "Infosys", "HDFC", "ICICI", "SBI", "Reliance", "Wipro", "HUL", "ITC", "Bajaj", "Kotak", "LT"]
    if stock_name not in valid_stocks:
        # Even for invalid stocks, we should show a proper error page instead of redirecting
        # This allows guests to see an error message rather than being redirected
        pass  # We'll handle this in the frontend
    
    # Log authentication status for debugging
    auth_status = {
        'authenticated': current_user.is_authenticated,
        'username': current_user.username if current_user.is_authenticated else None,
        'user_id': current_user.id if current_user.is_authenticated else None
    }
    logger.info(f"Stock analysis request - Stock: {stock_name}, Auth Status: {auth_status}")
    
    # Additional debugging for guest access
    logger.info(f"Current user authenticated: {current_user.is_authenticated}")
    
    # Ensure we're not redirecting guests
    if not current_user.is_authenticated:
        logger.info("Serving page for guest user - no redirect should occur")
    
    # Simple test - return a minimal page to see if the issue is with the Flask route or JavaScript
    if request.args.get('simple') == 'true':
        return f"""<html>
        <head>
            <title>Simple Stock Analysis</title>
        </head>
        <body>
            <h1>Simple Stock Analysis for {stock_name}</h1>
            <p>This is a test page to see if the redirect issue is with Flask or JavaScript.</p>
            <p>Auth Status: {auth_status}</p>
            <a href='/'>Back to Home</a>
        </body>
        </html>"""
    
    # Pass authentication context and stock name to template
    auth_data = {
        'authenticated': current_user.is_authenticated,
        'username': current_user.username if current_user.is_authenticated else None,
        'user_id': current_user.id if current_user.is_authenticated else None
    }
    
    # Read and modify the stock2.html template
    try:
        with open('stock2.html', 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Inject authentication data and stock name directly into the JavaScript
        injection_script = f'''
        <script>
        // Injected authentication and stock data
        window.authData = {json.dumps(auth_data)};
        window.stockName = "{stock_name}";
        // Ensure content is visible for guests
        window.guestAccessEnabled = true;
        </script>
        '''
        
        # Insert the script before the closing </head> tag
        content = content.replace('</head>', injection_script + '</head>')
        
        # Also modify the JavaScript to use the injected data
        content = content.replace(
            'const stock = getStockFromURL();',
            'const stock = window.stockName || getStockFromURL();'
        )
        
        # Inject authentication status directly
        content = content.replace(
            'let currentUser = null;',
            f'let currentUser = {json.dumps(auth_data)};'
        )
        
        return content
    except FileNotFoundError:
        return "Stock analysis page not found", 404

# Serve stock2.html (Stock Page) - Legacy route for compatibility
@app.route('/stock2.html')
def serve_stock2():
    stock = request.args.get('stock')
    if stock:
        return redirect(url_for('serve_stock_analysis', stock_name=stock))
    return send_from_directory('.', 'stock2.html')

# Serve static files
@app.route('/static/<path:path>')
def serve_static(path):
    return send_from_directory('static', path)

# Handle favicon requests
@app.route('/favicon.ico')
def favicon():
    return '', 204  # No Content response to suppress favicon errors

# Test route for debugging guest access
@app.route('/test_guest_access')
def test_guest_access():
    auth_status = {
        'authenticated': current_user.is_authenticated,
        'username': current_user.username if current_user.is_authenticated else None,
        'user_id': current_user.id if current_user.is_authenticated else None
    }
    return f"""<html>
    <head>
        <title>Test Guest Access</title>
    </head>
    <body>
        <h1>Test Guest Access</h1>
        <p>Auth Status: {auth_status}</p>
        <p>No redirect should occur</p>
        <p><a href='/'>Back to Home</a></p>
    </body>
    </html>"""

# Test route for form debugging
@app.route('/test_form')
def test_form():
    return send_from_directory('.', 'test_form.html')

if __name__ == '__main__':
    app.run(debug=True)