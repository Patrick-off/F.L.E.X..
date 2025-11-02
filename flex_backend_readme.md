# F.L.E.X. Backend API

![F.L.E.X. Logo](https://via.placeholder.com/200x60/22D3EE/FFFFFF?text=F.L.E.X.)

**Federated Learning & Exchange eXperience** - Backend API pour l'intelligence collective multi-IA.

## 📋 Table des Matières

- [Vue d'ensemble](#vue-densemble)
- [Architecture](#architecture)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Déploiement](#déploiement)
- [Monitoring](#monitoring)
- [Sécurité](#sécurité)
- [Tests](#tests)
- [Contributing](#contributing)

## 🎯 Vue d'ensemble

F.L.E.X. Backend est une API REST complète qui orchestre les dialogues entre plusieurs IA (GPT-5, Claude, Gemini, Grok) pour produire des consensus éclairés et transparents.

### Fonctionnalités Principales

- ✅ **Orchestration Multi-IA** - Coordination de 4+ modèles d'IA
- ✅ **Génération de Consensus** - Algorithmes de convergence avancés
- ✅ **WebSocket en temps réel** - Mises à jour live des débats
- ✅ **Rate Limiting** - Limitation par plan (Free, Pro, Researcher, Guardian)
- ✅ **Caching Redis** - Performance optimale
- ✅ **Queue Management** - Traitement asynchrone avec Bull
- ✅ **Authentication JWT** - Sécurité robuste
- ✅ **PostgreSQL** - Base de données relationnelle
- ✅ **Monitoring** - Prometheus + Grafana
- ✅ **Logs** - Elasticsearch + Kibana

## 🏗️ Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Client    │────▶│     Nginx    │────▶│   Backend   │
│  (Web/App)  │     │  (Reverse    │     │   (Node.js) │
└─────────────┘     │   Proxy)     │     └─────────────┘
                    └──────────────┘            │
                                                │
                    ┌──────────────────────────┼────────────┐
                    │                          │            │
                    ▼                          ▼            ▼
            ┌──────────────┐          ┌──────────┐  ┌──────────┐
            │  PostgreSQL  │          │  Redis   │  │ RabbitMQ │
            │  (Database)  │          │ (Cache)  │  │ (Queue)  │
            └──────────────┘          └──────────┘  └──────────┘
                    │
                    │
                    ▼
            ┌──────────────┐
            │   Worker     │
            │  (Queue      │
            │  Processing) │
            └──────────────┘
                    │
                    │
        ┌───────────┼───────────┬───────────┐
        ▼           ▼           ▼           ▼
    ┌───────┐  ┌───────┐  ┌────────┐  ┌──────┐
    │ GPT-5 │  │Claude │  │ Gemini │  │ Grok │
    └───────┘  └───────┘  └────────┘  └──────┘
```

### Stack Technologique

- **Runtime:** Node.js 20+
- **Framework:** Express.js
- **Database:** PostgreSQL 16
- **Cache:** Redis 7
- **Queue:** Bull + RabbitMQ
- **WebSocket:** Socket.io
- **Monitoring:** Prometheus + Grafana
- **Logs:** Elasticsearch + Kibana
- **Proxy:** Nginx
- **Container:** Docker + Docker Compose

## 🚀 Installation

### Prérequis

- Node.js >= 18.0.0
- PostgreSQL >= 14
- Redis >= 6
- Docker & Docker Compose (optionnel)

### Installation Locale

```bash
# Cloner le repository
git clone https://github.com/flex-ai/backend.git
cd backend

# Installer les dépendances
npm install

# Copier le fichier d'environnement
cp .env.example .env

# Éditer .env avec vos configurations
nano .env

# Initialiser la base de données
npm run migrate

# Démarrer le serveur en développement
npm run dev

# Ou en production
npm start
```

### Installation avec Docker

```bash
# Cloner le repository
git clone https://github.com/flex-ai/backend.git
cd backend

# Copier et configurer l'environnement
cp .env.example .env
nano .env

# Démarrer tous les services
docker-compose up -d

# Vérifier les logs
docker-compose logs -f backend

# Arrêter les services
docker-compose down
```

## ⚙️ Configuration

### Variables d'Environnement

Voir `.env.example` pour toutes les variables disponibles.

#### Configuration Minimale

```env
# Server
PORT=3000
NODE_ENV=production

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=flex_db
DB_USER=postgres
DB_PASSWORD=your_secure_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your_super_secret_jwt_key_min_256_bits

# AI APIs (au moins une requise)
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
```

### Plans Tarifaires

Configuration des limites par plan dans `server.js`:

```javascript
const limits = {
  free: 10,        // 10 requêtes/jour
  pro: 100,        // 100 requêtes/jour
  researcher: 500, // 500 requêtes/jour
  guardian: 999999 // Illimité (modéré)
};
```

## 📚 API Documentation

### Base URL

```
Production: https://api.flex.ai/v1
Development: http://localhost:3000/api/v1
```

### Authentication

Toutes les requêtes (sauf register/login) nécessitent un token JWT:

```bash
Authorization: Bearer <your_jwt_token>
```

### Endpoints Principaux

#### 1. Authentication

**POST /api/v1/auth/register**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "fullName": "John Doe"
}
```

**POST /api/v1/auth/login**
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

#### 2. Query Submission

**POST /api/v1/queries/submit**
```json
{
  "query": "Quel est l'impact du changement climatique sur l'économie?",
  "models": ["gpt5", "claude", "gemini", "grok"],
  "debate_rounds": 2,
  "consensus_threshold": 0.75
}
```

**Response:**
```json
{
  "query_id": "qry_1234567890_abc123",
  "status": "processing",
  "estimated_time": 15,
  "webhook_url": "wss://api.flex.ai/v1/ws/qry_1234567890_abc123",
  "daily_usage": {
    "current": 5,
    "limit": 100,
    "remaining": 95
  }
}
```

#### 3. Get Results

**GET /api/v1/queries/:queryId/results**

**Response:**
```json
{
  "query_id": "qry_1234567890_abc123",
  "status": "completed",
  "question": "Quel est l'impact du changement climatique...",
  "consensus": {
    "summary": "Le changement climatique a un impact...",
    "confidence": 0.87,
    "convergence_points": [
      "Augmentation des coûts d'assurance",
      "Migration économique vers énergies vertes"
    ],
    "divergence_points": [
      "Vitesse de la transition énergétique"
    ]
  },
  "individual_responses": {
    "gpt5": {
      "response": "Le changement climatique représente...",
      "confidence": 0.92,
      "reasoning": ["Historical data", "Economic models"]
    },
    "claude": { /* ... */ }
  },
  "metadata": {
    "processing_time": 14200,
    "created_at": "2025-10-31T10:30:00Z",
    "completed_at": "2025-10-31T10:30:14Z"
  }
}
```

#### 4. User Statistics

**GET /api/v1/user/stats**

**Response:**
```json
{
  "today": {
    "queries_count": 5,
    "tokens_used": 12450
  },
  "total_queries": 157,
  "avg_confidence": 0.86,
  "plan": "pro"
}
```

### WebSocket Events

Connectez-vous au WebSocket pour recevoir les mises à jour en temps réel:

```javascript
const socket = io('wss://api.flex.ai');

// Subscribe to query updates
socket.emit('subscribe_query', 'qry_1234567890_abc123');

// Listen for completion
socket.on('query_completed', (data) => {
  console.log('Query completed:', data);
});

// Listen for failures
socket.on('query_failed', (error) => {
  console.error('Query failed:', error);
});
```

### Error Codes

| Code | Erreur | Description |
|------|--------|-------------|
| 400 | Bad Request | Requête invalide |
| 401 | Unauthorized | Token manquant ou invalide |
| 403 | Forbidden | Accès refusé |
| 404 | Not Found | Ressource introuvable |
| 429 | Too Many Requests | Rate limit dépassé |
| 500 | Internal Server Error | Erreur serveur |

## 🚢 Déploiement

### Déploiement Docker (Recommandé)

```bash
# Build l'image
docker build -t flex-backend:latest .

# Run le container
docker run -d \
  --name flex-backend \
  -p 3000:3000 \
  --env-file .env \
  flex-backend:latest

# Avec docker-compose (tous les services)
docker-compose -f docker-compose.prod.yml up -d
```

### Déploiement sur AWS/GCP/Azure

1. **Build l'image Docker**
2. **Push vers Container Registry**
3. **Déployer sur:**
   - AWS: ECS/EKS + RDS + ElastiCache
   - GCP: Cloud Run + Cloud SQL + Memorystore
   - Azure: Container Instances + PostgreSQL + Redis Cache

### CI/CD avec GitHub Actions

Voir `.github/workflows/deploy.yml` pour l'intégration continue.

## 📊 Monitoring

### Prometheus Metrics

Métriques exposées sur `http://localhost:3000/metrics`:

- `http_requests_total` - Total requêtes HTTP
- `http_request_duration_seconds` - Durée des requêtes
- `queries_processed_total` - Total queries traitées
- `consensus_confidence_average` - Confiance moyenne
- `ai_api_errors_total` - Erreurs API IA

### Grafana Dashboards

Accédez à Grafana: `http://localhost:3001`

Dashboards préconfigurés:
- Overview général
- Performance API
- Usage par utilisateur
- Erreurs et alertes

### Logs

Elasticsearch + Kibana pour l'analyse des logs:
- `http://localhost:5601` (Kibana)
- Index patterns: `flex-logs-*`

## 🔒 Sécurité

### Bonnes Pratiques Implémentées

✅ **Helmet** - Headers de sécurité HTTP
✅ **Rate Limiting** - Protection DDoS
✅ **JWT** - Authentication sécurisée
✅ **bcrypt** - Hash de mots de passe
✅ **CORS** - Cross-Origin configuré
✅ **Input Validation** - Joi schemas
✅ **SQL Injection Protection** - Parameterized queries
✅ **XSS Protection** - Content Security Policy
✅ **HTTPS Only** - TLS 1.3

### Audit de Sécurité

```bash
# Audit npm packages
npm audit

# Fix vulnerabilities
npm audit fix

# Security scan
npm run security:scan
```

## 🧪 Tests

```bash
# Run tous les tests
npm test

# Tests avec coverage
npm run test:coverage

# Tests en watch mode
npm run test:watch

# Tests d'intégration
npm run test:integration

# Tests de performance
npm run test:perf
```

### Structure des Tests

```
tests/
├── unit/
│   ├── services/
│   ├── controllers/
│   └── utils/
├── integration/
│   ├── api/
│   └── database/
└── e2e/
    └── scenarios/
```

## 📈 Performance

### Optimisations

- **Caching Redis** - 60s TTL pour requêtes fréquentes
- **Connection Pooling** - PostgreSQL pool size: 20
- **Compression** - gzip pour toutes les réponses
- **Query Optimization** - Indexes sur colonnes fréquentes
- **Async Processing** - Queue Bull pour heavy tasks

### Benchmarks

```
GET /api/v1/queries (cached):    ~50ms
POST /api/v1/queries/submit:     ~100ms
Query Processing (full):         ~15s
WebSocket latency:               ~10ms
```

## 🤝 Contributing

Voir [CONTRIBUTING.md](CONTRIBUTING.md) pour les guidelines de contribution.

### Development Workflow

1. Fork le projet
2. Créer une branche (`git checkout -b feature/amazing-feature`)
3. Commit (`git commit -m 'Add amazing feature'`)
4. Push (`git push origin feature/amazing-feature`)
5. Ouvrir une Pull Request

## 📄 License

Propriétaire © 2025 F.L.E.X. - Tous droits réservés.

Ce code est protégé et ne peut être utilisé, copié, modifié ou distribué sans autorisation explicite.

## 📞 Support

- **Email:** support@flex.ai
- **Documentation:** https://docs.flex.ai
- **Discord:** https://discord.gg/flex-ai
- **Status:** https://status.flex.ai

## 🙏 Remerciements

Merci à tous les contributeurs et à la communauté open-source.

---

**Made with 💙 by the F.L.E.X. Team**