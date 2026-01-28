from flask import Blueprint, request, jsonify
import sqlite3

auth_bp = Blueprint("auth", __name__)

@auth_bp.route("/login", methods=["POST"])
def login():
    dados = request.json
    username = dados.get("username")
    password = dados.get("password")

    conexao = sqlite3.connect("financeiro.db")
    cursor = conexao.cursor()

    cursor.execute("SELECT * FROM usuarios WHERE username=? AND password=?", (username, password))
    usuario = cursor.fetchone()
    conexao.close()

    if usuario:
        return jsonify({"mensagem": "Login realizado com sucesso!", "usuario_id": usuario[0]})
    return jsonify({"mensagem": "Usuário ou senha inválidos"}), 401
