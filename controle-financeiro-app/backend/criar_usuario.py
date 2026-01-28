import sqlite3

conexao = sqlite3.connect("financeiro.db")
cursor = conexao.cursor()

cursor.execute("""
INSERT INTO usuarios (username, password)
VALUES (?, ?)
""", ("admin", "1234"))

conexao.commit()
conexao.close()

print("Usuário criado com sucesso!")
