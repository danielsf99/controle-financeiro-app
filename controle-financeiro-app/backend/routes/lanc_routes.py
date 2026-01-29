from flask import Blueprint, request, jsonify
import sqlite3

lanc_bp = Blueprint("lancamentos", __name__)

# -------- CRIAR --------
@lanc_bp.route("/lancamentos", methods=["POST"])
def criar_lancamento():
    dados = request.json
    conexao = sqlite3.connect("financeiro.db")
    cursor = conexao.cursor()

    cursor.execute("""
    INSERT INTO lancamentos (descricao, valor, tipo, data, observacao, usuario_id)
    VALUES (?, ?, ?, ?, ?, ?)
    """, (
        dados["descricao"],
        dados["valor"],
        dados["tipo"],
        dados["data"],
        dados.get("observacao"),
        dados["usuario_id"]
    ))

    conexao.commit()
    conexao.close()
    return jsonify({"mensagem": "Lançamento criado com sucesso!"})


# -------- LISTAR TODOS DO USUÁRIO --------
@lanc_bp.route("/lancamentos/<int:usuario_id>", methods=["GET"])
def listar_lancamentos(usuario_id):
    conexao = sqlite3.connect("financeiro.db")
    cursor = conexao.cursor()

    cursor.execute("SELECT * FROM lancamentos WHERE usuario_id=?", (usuario_id,))
    lancamentos = cursor.fetchall()
    conexao.close()

    lista = []
    for l in lancamentos:
        lista.append({
            "id": l[0],
            "descricao": l[1],
            "valor": l[2],
            "tipo": l[3],
            "data": l[4],
            "observacao": l[5]
        })

    return jsonify(lista)


# -------- VER UM LANÇAMENTO --------
@lanc_bp.route("/lancamento/<int:id>", methods=["GET"])
def ver_lancamento(id):
    conexao = sqlite3.connect("financeiro.db")
    cursor = conexao.cursor()

    cursor.execute("SELECT * FROM lancamentos WHERE id=?", (id,))
    l = cursor.fetchone()
    conexao.close()

    if l:
        return jsonify({
            "id": l[0],
            "descricao": l[1],
            "valor": l[2],
            "tipo": l[3],
            "data": l[4],
            "observacao": l[5]
        })

    return jsonify({"mensagem": "Não encontrado"}), 404


# -------- ATUALIZAR --------
@lanc_bp.route("/lancamento/<int:id>", methods=["PUT"])
def atualizar_lancamento(id):
    dados = request.json

    conexao = sqlite3.connect("financeiro.db")
    cursor = conexao.cursor()

    cursor.execute("""
    UPDATE lancamentos
    SET descricao=?, valor=?, tipo=?, data=?, observacao=?
    WHERE id=?
    """, (
        dados["descricao"],
        dados["valor"],
        dados["tipo"],
        dados["data"],
        dados.get("observacao"),
        id
    ))

    conexao.commit()
    conexao.close()

    return jsonify({"mensagem": "Atualizado com sucesso"})


# -------- DELETAR --------
@lanc_bp.route("/lancamento/<int:id>", methods=["DELETE"])
def deletar_lancamento(id):
    conexao = sqlite3.connect("financeiro.db")
    cursor = conexao.cursor()

    cursor.execute("DELETE FROM lancamentos WHERE id=?", (id,))
    conexao.commit()
    conexao.close()

    return jsonify({"mensagem": "Deletado com sucesso"})

