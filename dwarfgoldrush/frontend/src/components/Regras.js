import React from 'react';
import './Regras.css';

function Regras() {
  return (
    <div className="regras-container">
      <h2>📜 Regras de Recompensa — Dwarf Gold Rush</h2>

      <h3>🎯 Metas e Recompensas</h3>
      <ul>
                    </ul>

      <h3>👥 Recompensas por Convites</h3>
      <ul>
        <li>🔹 <strong>1 Ecopact</strong> por novo usuário convidado</li>
        <li>🔹 <strong>10 convidados</strong> → +10 Ecopacts</li>
      </ul>

      <h3>📊 Recompensas por Atividade Mensal</h3>
      <ul>
        <li>🔹 <strong>100 usuários ativos/mês</strong> → +20 Ecopacts</li>
        <li>🔹 <strong>1000 usuários ativos/mês</strong> → +100 Ecopacts</li>
      </ul>

      <h3>🪙 Informações do Token</h3>
      <ul>
        <li><strong>Endereço na Blockchain</strong>: <code>0x3a713ad93a08AE1A7331b2Be5c1DaAD3f2Bf732e</code></li>
        <li><strong>Tipo do Token</strong>: ERC20</li>
        <li><strong>Rede do Token</strong>: Binance Smart Chain (BSCTEST)</li>
      </ul>
    </div>
  );
}

export default Regras;
