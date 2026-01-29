from flask import Flask

app = Flask(__name__)  # cria o servidor

@app.route("/")
def home():
    return "Servidor Flask está funcionando!"

if __name__ == "__main__":
    app.run()