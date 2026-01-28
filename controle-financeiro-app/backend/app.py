app = Flask(_name_)  # cria o servidor

@app.route("/")
def home():
    return "Servidor Flask está funcionando!"

if _name_ == "_main_":
    app.run()