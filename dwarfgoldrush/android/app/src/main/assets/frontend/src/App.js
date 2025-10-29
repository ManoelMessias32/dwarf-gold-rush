import React, { useState, useEffect } from 'react';
import TelegramWebApp from '@twa-dev/sdk';
import useSound from 'use-sound';
import hammerSound from './sounds/hammer.mp3';
import caveSound from './sounds/cave.mp3';
import claimSound from './sounds/claim.mp3';
import Regras from './components/Regras';
import './App.css';

TelegramWebApp.ready();
const user = TelegramWebApp.initDataUnsafe.user || { id: 123, username: 'Test' };

function App() {
  const [nuggets, setNuggets] = useState(0);
  const [totalMined, setTotalMined] = useState(0);
  const [cooldown, setCooldown] = useState(false);
  const [loading, setLoading] = useState(false);
  const [muted, setMuted] = useState(false);
  const [showRegras, setShowRegras] = useState(false);
  const [adsWatched, setAdsWatched] = useState(0);
  const [progressStartTime, setProgressStartTime] = useState(null);



  // Sons
  const [playHammer] = useSound(hammerSound, { volume: muted ? 0 : 0.5 });
  // eslint-disable-next-line no-unused-vars
  const [playClaim] = useSound(claimSound, { volume: muted ? 0 : 0.6 });
  const [playCave] = useSound(caveSound, { volume: muted ? 0 : 0.3, loop: true });

  // Efeito para som de fundo e busca de usuário
  useEffect(() => {
    playCave(); // Remove a verificação if para evitar problemas de inicialização
    fetchUser();
    const interval = setInterval(fetchUser, 1000);
    return () => clearInterval(interval);
  }, [playCave]); // Array de dependências vazio

  const fetchUser = async () => {
    try {
      const res = await fetch('http://localhost:3002/user', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ telegram_id: user.id })
      });
      if (!res.ok) throw new Error(`HTTP error! Status: ${res.status}`);
      const data = await res.json();
      console.log('Dados do usuário:', data); // Debug
      setNuggets(data.nuggets || 0);
      setTotalMined(data.total_mined || 0);
    } catch (error) {
      console.error('Erro ao buscar usuário:', error);
    }
  };

  const mine = async () => {
    if (cooldown) return;
    playHammer();
    setCooldown(true);
    setLoading(true);
    try {
      const res = await fetch('http://localhost:3002/mine', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ telegram_id: user.id, username: user.username })
      });
      const data = await res.json();
      if (data.success) {
        setNuggets(prev => prev + data.nuggets);
        setTotalMined(prev => prev + data.nuggets);
      }
    } catch (error) {
      console.error('Erro ao minerar:', error);
    } finally {
      setTimeout(() => {
        setCooldown(false);
        setLoading(false);
      }, 3000);
    }
  };


  const abrirRegras = () => {
    setShowRegras(true);
  };

  const assistirAnuncio = () => {
    setAdsWatched(prev => prev + 1);
    if (adsWatched + 1 === 20) {
      setProgressStartTime(Date.now()); // Inicia contagem de 8h
    }
  };

  const podeRecarregar = () => {
    if (!progressStartTime) return false;
    const agora = Date.now();
    return agora - progressStartTime >= 4 * 60 * 60 * 1000; // 4 horas (ajustado para teste)
  };

  const claimDailyReward = () => {
    if (adsWatched < 20) {
      alert('Você precisa assistir 20 anúncios!');
      return;
    }
    if (!podeRecarregar()) {
      alert('Aguarde 4 horas após os 20 anúncios!');
      return;
    }
    alert('🎉 3 ECOPACT (Diário) Reivindicado!');
    setProgressStartTime(null);
    setAdsWatched(0);
  };

  const progressoBarra = () => {
    if (!progressStartTime) return 0;
    const tempoPassado = Date.now() - progressStartTime;
    const duracao = 8 * 60 * 60 * 1000; // 8 horas
    return Math.min(100, (tempoPassado / duracao) * 100).toFixed(0);
  };

  return (
    <div className="App">
      <h1>DWARF GOLD RUSH</h1>

      {showRegras && <Regras onClose={() => setShowRegras(false)} />}

      <div className="stats">
        <div>Nuggets: {nuggets.toLocaleString()}</div>
        <div>Total: {totalMined.toLocaleString()}</div>
        <div>ECOPACT: {((totalMined / 20000).toFixed(2))}</div>
        <div>Anúncios assistidos: {adsWatched}/20</div>
        {progressStartTime && (
          <><div>Barra de progresso: {progressoBarra()}%</div><div className="barra-anuncios">
            <label>🎥 Progresso de Anúncios</label>
            <div className="barra-visual">
              <div className="barra-preenchida" style={{ width: `${(adsWatched / 20) * 100}%` }}></div>
            </div>
            <span>{adsWatched}/20</span>
          </div></>

        )}
      </div>

      <button onClick={mine} disabled={cooldown || loading} className="mine-btn">
        {loading ? 'MINERANDO...' : 'MINERAR'}
      </button>

      <button onClick={assistirAnuncio} className="ad-btn">
        Assistir Anúncio (+1)
      </button>

      {totalMined >= 10000000 && (
        <button onClick={claimDailyReward} className="claim-btn">
          CLAIM 3 ECOPACT (Diário)
        </button>
      )}

      <div className="ads">
        <ins className="adsbygoogle"
             data-ad-client="ca-pub-1262202142447431"
             data-ad-slot="7738751536"></ins>
      </div>

      <div className="ui-buttons">
        <button className="volume-btn" onClick={() => setMuted(prev => !prev)}>
          {muted ? 'Mudo' : 'Som'}
        </button>
        <button className="rules-btn" onClick={abrirRegras} title="Regras">
          Regras
        </button>
      </div>
    </div>
  );
}

export default App;