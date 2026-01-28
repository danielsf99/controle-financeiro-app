from flask import Flask
from database import criar_tabelas
from routes.auth_routes import auth_bp
from routes.lanc_routes import lanc_bp

app = Flask(__name__)
criar_tabelas()

app.register_blueprint(auth_bp)
app.register_blueprint(lanc_bp)

@app.route("/")
def home():
    return "Backend do Controle Financeiro está rodando!"

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
