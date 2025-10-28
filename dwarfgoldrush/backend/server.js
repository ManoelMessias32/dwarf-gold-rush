const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const cors = require('cors');
const { Web3 } = require('web3');

require('dotenv').config();

const app = express();
app.use(cors()); // Adicione após const app = express()
app.use(express.json());
const bscWeb3 = new Web3('https://data-seed-prebsc-1-s1.binance.org:8545/');
const ECOPACT_ADDRESS = '0x3a713ad93a08AE1A7331b2Be5c1DaAD3f2Bf732e';

const db = new sqlite3.Database('../database/users.db');
db.run(`CREATE TABLE IF NOT EXISTS users (
  telegram_id TEXT PRIMARY KEY,
  username TEXT,
  nuggets INTEGER DEFAULT 0,
  last_mine INTEGER DEFAULT 0,
  total_mined INTEGER DEFAULT 0,
  ecopact_claimed INTEGER DEFAULT 0,
  invited_users INTEGER DEFAULT 0,
  last_ad_check INTEGER DEFAULT 0
)`);

app.post('/mine', (req, res) => {
  const { telegram_id, username } = req.body;
  const now = Date.now();

  db.get('SELECT * FROM users WHERE telegram_id = ?', [telegram_id], (err, user) => {
    if (!user) {
      db.run('INSERT INTO users (telegram_id, username) VALUES (?, ?)', [telegram_id, username]);
      user = { nuggets: 0, last_mine: 0, total_mined: 0, last_ad_check: 0 };
    }

    if (now - user.last_mine < 3000) {
      return res.json({ success: false, message: 'Espere 3s!' });
    }

    if (user.total_mined % 10000 === 0 && now - user.last_ad_check > 60000) {
      return res.json({ success: false, message: 'Assista a um anúncio para continuar!' });
    }

    const nuggets = Math.floor(Math.random() * 5) + 1;
    db.run(`UPDATE users SET nuggets = nuggets + ?, last_mine = ?, total_mined = total_mined + ? WHERE telegram_id = ?`,
      [nuggets, now, nuggets, telegram_id]);

    res.json({ success: true, nuggets, total: user.total_mined + nuggets });
  });
});

app.post('/ad-viewed', (req, res) => {
  const { telegram_id } = req.body;
  const now = Date.now();
  db.run(`UPDATE users SET last_ad_check = ? WHERE telegram_id = ?`, [now, telegram_id]);
  res.json({ success: true });
});

app.post('/claim', (req, res) => {
  const { telegram_id } = req.body;
  db.get('SELECT * FROM users WHERE telegram_id = ?', [telegram_id], (err, user) => {
    if (user.total_mined < 1000000) {
      return res.json({ success: false, needed: 1000000 - user.total_mined });
    }
    if (user.ecopact_claimed >= 60) {
      return res.json({ success: false, message: 'Limite de 60 Ecopacts atingido!' });
    }
    db.run(`UPDATE users SET ecopact_claimed = ecopact_claimed + 5 WHERE telegram_id = ?`, [telegram_id]);
    res.json({ success: true, txHash: '0x123...' });
  });
});

app.post('/invite', (req, res) => {
  const { inviter_id, new_user_id, username } = req.body;
  db.get('SELECT * FROM users WHERE telegram_id = ?', [new_user_id], (err, user) => {
    if (!user) {
      db.run('INSERT INTO users (telegram_id, username) VALUES (?, ?)', [new_user_id, username]);
      db.run(`UPDATE users SET invited_users = invited_users + 1, ecopact_claimed = ecopact_claimed + 1 WHERE telegram_id = ?`, [inviter_id]);
      return res.json({ success: true, message: 'Usuário convidado com sucesso!' });
    } else {
      return res.json({ success: false, message: 'Usuário já existe!' });
    }
  });
});

app.post('/user', (req, res) => {
  const { telegram_id } = req.body;
  db.get('SELECT * FROM users WHERE telegram_id = ?', [telegram_id], (err, user) => {
    res.json(user || { nuggets: 0, total_mined: 0, ecopact_claimed: 0 });
  });
});

app.get('/relatorio', (req, res) => {
  const dias = 30;
  const limite = Date.now() - dias * 24 * 60 * 60 * 1000;

  db.all(`SELECT telegram_id, username, total_mined, ecopact_claimed, last_mine, last_ad_check FROM users`, [], (err, rows) => {
    const ativos = rows.filter(u => u.last_mine > limite || u.last_ad_check > limite);
    res.json({
      totalUsuarios: rows.length,
      ativos: ativos.length,
      lista: rows
    });
  });
});

app.listen(3002, () => console.log('🚀 Backend: http://localhost:3002'));


