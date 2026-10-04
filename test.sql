-- =========================================================
-- 1. Création de la base (à exécuter depuis une autre base, ex: postgres)
-- =========================================================
-- ⚠️ CREATE DATABASE ne peut PAS être dans un bloc transactionnel
CREATE DATABASE sprint;

-- =========================================================
-- 2. Définir le mot de passe du user postgres
-- =========================================================
ALTER USER postgres WITH PASSWORD 'password';

-- =========================================================
-- 3. Se connecter à la nouvelle base
-- =========================================================
\c sprint

-- =========================================================
-- 4. Table de test simple
-- =========================================================
CREATE TABLE IF NOT EXISTS test_framework (
    id          SERIAL PRIMARY KEY,
    message     VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- 5. Données de test
-- =========================================================
INSERT INTO test_framework (message) VALUES
    ('Hello depuis le framework !'),
    ('Connexion PostgreSQL OK'),
    ('Test réussi 🎉');

-- =========================================================
-- 6. Vérification
-- =========================================================
SELECT * FROM test_framework;