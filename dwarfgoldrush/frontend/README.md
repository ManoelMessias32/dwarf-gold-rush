# 🪙 Dwarf Gold Rush

Um jogo web de mineração onde você coleta nuggets, assiste anúncios e ganha Ecopacts como recompensa. Desenvolvido com Node.js, SQLite, Web3 e integração com Telegram.

---

## 🚀 Funcionalidades

- ⛏️ Mineração com tempo de espera
- 📺 Recompensas por anúncios assistidos
- 🎁 Sistema de Ecopacts por progresso e convites
- 👥 Painel de administração com relatório de usuários ativos
- 🤖 Integração com bot do Telegram (em breve)

---

## 📦 Tecnologias

| Camada     | Tecnologias                     |
|------------|----------------------------------|
| Backend    | Express, SQLite, Web3            |
| Frontend   | React + HTML + JS                |
| Blockchain | Binance Smart Chain (testnet)    |
| Bot        | Telegram (em desenvolvimento)    |

---

## 🧠 Regras de recompensa

- 🔹 1.000.000 nuggets → 5 Ecopacts
- 🔹 Máximo de 60 Ecopacts por jogador
- 🔹 A cada 10.000 nuggets → 1 anúncio obrigatório
- 🔹 1 Ecopact por novo usuário convidado
- 🔹 10 convidados → +10 Ecopacts
- 🔹 100 usuários ativos por mês → +20 Ecopacts
- 🔹 1000 usuários ativos por mês → +100 Ecopacts

---

## 📊 Painel de administração

Visualize os dados dos jogadores com filtros e destaques:

![Painel de administração](./screenshots/mining-scene.png)
![Bot do Telegram em ação](./screenshots/telegram-bot.png)

---

## ▶️ Jogar Agora

> *(Se você tiver uma versão hospedada, adicione o link abaixo)*

[🔗 Jogar Dwarf Gold Rush](http://localhost:3000)

---

## 🧑‍💻 Como rodar localmente

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/dwarf-gold-rush.git
cd dwarf-gold-rush

# Instale as dependências
cd backend
npm install
node server.js

# Em outro terminal
cd ../frontend
npm install
npm start
