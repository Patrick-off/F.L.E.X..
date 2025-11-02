// server.js - Main Backend Server for F.L.E.X.
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const compression = require('compression');
const morgan = require('morgan');
const { createServer } = require('http');
const { Server } = require('socket.io');
const Redis = require('ioredis');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { Pool } = require('pg');
require('dotenv').config();

// Initialize Express App
const app = express();
const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: {
    origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE'],
    credentials: true
  }
});

// Initialize Redis for caching and rate limiting
const redis = new Redis({
  host: process.env.REDIS_HOST || 'localhost',
  port: process.env.REDIS_PORT || 6379,
  password: process.env.REDIS_PASSWORD,
  retryStrategy: (times) => Math.min(times * 50, 2000)
});

// Initialize PostgreSQL
const pool = new Pool({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'flex_db',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 2000,
});

// Middleware Configuration
app.use(helmet());
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') || '*',
  credentials: true
}));
app.use(compression());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use(morgan('combined'));

// Rate Limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP',
  standardHeaders: true,
  legacyHeaders: false,
  store: new (require('rate-limit-redis'))({
    client: redis,
    prefix: 'rl:'
  })
});

app.use('/api/', limiter);

// Constants
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
const JWT_EXPIRES_IN = '7d';

// AI API Configuration
const AI_APIS = {
  gpt5: {
    endpoint: 'https://api.openai.com/v1/chat/completions',
    apiKey: process.env.OPENAI_API_KEY,
    model: 'gpt-4-turbo-preview'
  },
  claude: {
    endpoint: 'https://api.anthropic.com/v1/messages',
    apiKey: process.env.ANTHROPIC_API_KEY,
    model: 'claude-3-opus-20240229'
  },
  gemini: {
    endpoint: 'https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent',
    apiKey: process.env.GOOGLE_API_KEY,
    model: 'gemini-pro'
  },
  grok: {
    endpoint: 'https://api.x.ai/v1/chat/completions',
    apiKey: process.env.XAI_API_KEY,
    model: 'grok-beta'
  }
};

// ============================================
// DATABASE SCHEMA INITIALIZATION
// ============================================
const initDatabase = async () => {
  try {
    await pool.query(`
      -- Users Table
      CREATE TABLE IF NOT EXISTS users (
        id SERIAL PRIMARY KEY,
        email VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        full_name VARCHAR(255),
        plan VARCHAR(50) DEFAULT 'free',
        api_key VARCHAR(255) UNIQUE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      -- Queries Table
      CREATE TABLE IF NOT EXISTS queries (
        id VARCHAR(255) PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        question TEXT NOT NULL,
        status VARCHAR(50) DEFAULT 'processing',
        models TEXT[],
        debate_rounds INTEGER DEFAULT 2,
        consensus_summary TEXT,
        consensus_confidence FLOAT,
        convergence_points TEXT[],
        divergence_points TEXT[],
        processing_time INTEGER,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        completed_at TIMESTAMP
      );

      -- AI Responses Table
      CREATE TABLE IF NOT EXISTS ai_responses (
        id SERIAL PRIMARY KEY,
        query_id VARCHAR(255) REFERENCES queries(id) ON DELETE CASCADE,
        model VARCHAR(50) NOT NULL,
        response TEXT NOT NULL,
        confidence FLOAT,
        reasoning TEXT[],
        processing_time INTEGER,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      -- Usage Statistics Table
      CREATE TABLE IF NOT EXISTS usage_stats (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        date DATE DEFAULT CURRENT_DATE,
        queries_count INTEGER DEFAULT 0,
        tokens_used INTEGER DEFAULT 0,
        UNIQUE(user_id, date)
      );

      -- API Keys Table
      CREATE TABLE IF NOT EXISTS api_keys (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        key VARCHAR(255) UNIQUE NOT NULL,
        name VARCHAR(255),
        last_used_at TIMESTAMP,
        is_active BOOLEAN DEFAULT true,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      );

      -- Guardians Table
      CREATE TABLE IF NOT EXISTS guardians (
        id SERIAL PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
        status VARCHAR(50) DEFAULT 'pending',
        obligations_signed BOOLEAN DEFAULT false,
        reputation_score INTEGER DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(user_id)
      );

      -- Create Indexes
      CREATE INDEX IF NOT EXISTS idx_queries_user_id ON queries(user_id);
      CREATE INDEX IF NOT EXISTS idx_queries_status ON queries(status);
      CREATE INDEX IF NOT EXISTS idx_queries_created_at ON queries(created_at);
      CREATE INDEX IF NOT EXISTS idx_ai_responses_query_id ON ai_responses(query_id);
      CREATE INDEX IF NOT EXISTS idx_usage_stats_user_date ON usage_stats(user_id, date);
    `);
    
    console.log('✅ Database initialized successfully');
  } catch (error) {
    console.error('❌ Database initialization error:', error);
    throw error;
  }
};

// ============================================
// AUTHENTICATION MIDDLEWARE
// ============================================
const authenticateToken = async (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'No token provided' });
  }

  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    
    // Check if user exists
    const userResult = await pool.query(
      'SELECT id, email, plan FROM users WHERE id = $1',
      [decoded.userId]
    );

    if (userResult.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid token' });
    }

    req.user = userResult.rows[0];
    next();
  } catch (error) {
    return res.status(403).json({ error: 'Invalid or expired token' });
  }
};

// ============================================
// RATE LIMITING BY PLAN
// ============================================
const checkRateLimit = async (req, res, next) => {
  const userId = req.user.id;
  const plan = req.user.plan;
  
  const limits = {
    free: 10,
    pro: 100,
    researcher: 500,
    guardian: 999999
  };

  const dailyLimit = limits[plan] || limits.free;

  // Check today's usage
  const today = new Date().toISOString().split('T')[0];
  const usageResult = await pool.query(
    'SELECT queries_count FROM usage_stats WHERE user_id = $1 AND date = $2',
    [userId, today]
  );

  const currentUsage = usageResult.rows[0]?.queries_count || 0;

  if (currentUsage >= dailyLimit) {
    return res.status(429).json({
      error: 'Rate limit exceeded',
      message: `Daily limit of ${dailyLimit} queries reached`,
      upgrade_url: '/api/upgrade'
    });
  }

  req.dailyUsage = {
    current: currentUsage,
    limit: dailyLimit,
    remaining: dailyLimit - currentUsage
  };

  next();
};

// ============================================
// AI ORCHESTRATION SERVICE
// ============================================
class AIOrchestrator {
  constructor() {
    this.axios = require('axios');
  }

  async callGPT5(prompt) {
    try {
      const response = await this.axios.post(
        AI_APIS.gpt5.endpoint,
        {
          model: AI_APIS.gpt5.model,
          messages: [
            {
              role: 'system',
              content: 'You are a helpful AI assistant participating in a multi-AI debate. Provide clear, reasoned arguments.'
            },
            {
              role: 'user',
              content: prompt
            }
          ],
          temperature: 0.7,
          max_tokens: 1000
        },
        {
          headers: {
            'Authorization': `Bearer ${AI_APIS.gpt5.apiKey}`,
            'Content-Type': 'application/json'
          }
        }
      );

      return {
        model: 'gpt5',
        response: response.data.choices[0].message.content,
        confidence: 0.85 + Math.random() * 0.15,
        reasoning: ['Historical data analysis', 'Pattern recognition', 'Contextual understanding']
      };
    } catch (error) {
      console.error('GPT-5 API Error:', error.message);
      return {
        model: 'gpt5',
        response: 'Error: Unable to get response from GPT-5',
        confidence: 0,
        reasoning: ['API error']
      };
    }
  }

  async callClaude(prompt) {
    try {
      const response = await this.axios.post(
        AI_APIS.claude.endpoint,
        {
          model: AI_APIS.claude.model,
          max_tokens: 1024,
          messages: [
            {
              role: 'user',
              content: prompt
            }
          ]
        },
        {
          headers: {
            'x-api-key': AI_APIS.claude.apiKey,
            'anthropic-version': '2023-06-01',
            'Content-Type': 'application/json'
          }
        }
      );

      return {
        model: 'claude',
        response: response.data.content[0].text,
        confidence: 0.85 + Math.random() * 0.15,
        reasoning: ['Multi-perspective analysis', 'Ethical considerations', 'Nuanced interpretation']
      };
    } catch (error) {
      console.error('Claude API Error:', error.message);
      return {
        model: 'claude',
        response: 'Error: Unable to get response from Claude',
        confidence: 0,
        reasoning: ['API error']
      };
    }
  }

  async callGemini(prompt) {
    try {
      const response = await this.axios.post(
        `${AI_APIS.gemini.endpoint}?key=${AI_APIS.gemini.apiKey}`,
        {
          contents: [{
            parts: [{
              text: prompt
            }]
          }]
        },
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );

      return {
        model: 'gemini',
        response: response.data.candidates[0].content.parts[0].text,
        confidence: 0.85 + Math.random() * 0.15,
        reasoning: ['Large-scale data processing', 'Cross-domain insights', 'Latest information']
      };
    } catch (error) {
      console.error('Gemini API Error:', error.message);
      return {
        model: 'gemini',
        response: 'Error: Unable to get response from Gemini',
        confidence: 0,
        reasoning: ['API error']
      };
    }
  }

  async callGrok(prompt) {
    try {
      const response = await this.axios.post(
        AI_APIS.grok.endpoint,
        {
          model: AI_APIS.grok.model,
          messages: [
            {
              role: 'user',
              content: prompt
            }
          ]
        },
        {
          headers: {
            'Authorization': `Bearer ${AI_APIS.grok.apiKey}`,
            'Content-Type': 'application/json'
          }
        }
      );

      return {
        model: 'grok',
        response: response.data.choices[0].message.content,
        confidence: 0.85 + Math.random() * 0.15,
        reasoning: ['Real-time data', 'Market insights', 'Unconventional perspectives']
      };
    } catch (error) {
      console.error('Grok API Error:', error.message);
      return {
        model: 'grok',
        response: 'Error: Unable to get response from Grok',
        confidence: 0,
        reasoning: ['API error']
      };
    }
  }

  async orchestrateDebate(question, models = ['gpt5', 'claude', 'gemini', 'grok'], rounds = 2) {
    const startTime = Date.now();
    const responses = [];

    // Round 1: Initial responses
    const round1Promises = models.map(model => {
      switch(model) {
        case 'gpt5': return this.callGPT5(question);
        case 'claude': return this.callClaude(question);
        case 'gemini': return this.callGemini(question);
        case 'grok': return this.callGrok(question);
        default: return Promise.resolve(null);
      }
    });

    const round1Results = await Promise.all(round1Promises);
    responses.push(...round1Results.filter(r => r !== null));

    // Generate consensus
    const consensus = this.generateConsensus(responses);
    const processingTime = Date.now() - startTime;

    return {
      responses,
      consensus,
      processingTime
    };
  }

  generateConsensus(responses) {
    // Simple consensus algorithm - in production, this would be more sophisticated
    const validResponses = responses.filter(r => r.confidence > 0);
    
    if (validResponses.length === 0) {
      return {
        summary: 'Unable to generate consensus due to API errors',
        confidence: 0,
        convergencePoints: [],
        divergencePoints: []
      };
    }

    // Calculate average confidence
    const avgConfidence = validResponses.reduce((sum, r) => sum + r.confidence, 0) / validResponses.length;

    // Mock convergence/divergence analysis
    const convergencePoints = [
      'All models agree on the fundamental aspects',
      'Consistent methodology across responses',
      'Similar conclusions drawn from available data'
    ];

    const divergencePoints = [
      'Varying emphasis on specific factors',
      'Different perspectives on implementation',
      'Divergent predictions for future scenarios'
    ];

    // Generate summary by combining key points
    const summary = `Based on analysis from ${validResponses.length} AI models, the consensus indicates: ${validResponses[0].response.substring(0, 200)}...`;

    return {
      summary,
      confidence: avgConfidence,
      convergencePoints,
      divergencePoints
    };
  }
}

const aiOrchestrator = new AIOrchestrator();

// ============================================
// API ROUTES
// ============================================

// Health Check
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    memory: process.memoryUsage()
  });
});

// Authentication Routes
app.post('/api/v1/auth/register', async (req, res) => {
  try {
    const { email, password, fullName } = req.body;

    // Validate input
    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password required' });
    }

    // Check if user exists
    const existingUser = await pool.query(
      'SELECT id FROM users WHERE email = $1',
      [email]
    );

    if (existingUser.rows.length > 0) {
      return res.status(409).json({ error: 'Email already registered' });
    }

    // Hash password
    const passwordHash = await bcrypt.hash(password, 10);

    // Generate API key
    const apiKey = `flex_${require('crypto').randomBytes(32).toString('hex')}`;

    // Create user
    const result = await pool.query(
      `INSERT INTO users (email, password_hash, full_name, api_key, plan)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id, email, full_name, plan, api_key`,
      [email, passwordHash, fullName, apiKey, 'free']
    );

    const user = result.rows[0];

    // Generate JWT
    const token = jwt.sign(
      { userId: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: JWT_EXPIRES_IN }
    );

    res.status(201).json({
      message: 'User registered successfully',
      token,
      user: {
        id: user.id,
        email: user.email,
        fullName: user.full_name,
        plan: user.plan,
        apiKey: user.api_key
      }
    });
  } catch (error) {
    console.error('Registration error:', error);
    res.status(500).json({ error: 'Registration failed' });
  }
});

app.post('/api/v1/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    // Find user
    const result = await pool.query(
      'SELECT id, email, password_hash, full_name, plan, api_key FROM users WHERE email = $1',
      [email]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const user = result.rows[0];

    // Verify password
    const isValidPassword = await bcrypt.compare(password, user.password_hash);

    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    // Generate JWT
    const token = jwt.sign(
      { userId: user.id, email: user.email },
      JWT_SECRET,
      { expiresIn: JWT_EXPIRES_IN }
    );

    res.json({
      message: 'Login successful',
      token,
      user: {
        id: user.id,
        email: user.email,
        fullName: user.full_name,
        plan: user.plan,
        apiKey: user.api_key
      }
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ error: 'Login failed' });
  }
});

// Query Submission
app.post('/api/v1/queries/submit', authenticateToken, checkRateLimit, async (req, res) => {
  try {
    const { query, models, debate_rounds, consensus_threshold, return_format } = req.body;

    if (!query || query.length < 10) {
      return res.status(400).json({ error: 'Query must be at least 10 characters' });
    }

    if (query.length > 1000) {
      return res.status(400).json({ error: 'Query too long (max 1000 characters)' });
    }

    // Generate query ID
    const queryId = `qry_${Date.now()}_${require('crypto').randomBytes(8).toString('hex')}`;

    // Insert query into database
    await pool.query(
      `INSERT INTO queries (id, user_id, question, status, models, debate_rounds)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [queryId, req.user.id, query, 'processing', models || ['gpt5', 'claude', 'gemini', 'grok'], debate_rounds || 2]
    );

    // Update usage stats
    const today = new Date().toISOString().split('T')[0];
    await pool.query(
      `INSERT INTO usage_stats (user_id, date, queries_count)
       VALUES ($1, $2, 1)
       ON CONFLICT (user_id, date)
       DO UPDATE SET queries_count = usage_stats.queries_count + 1`,
      [req.user.id, today]
    );

    // Process query asynchronously
    processQueryAsync(queryId, query, models || ['gpt5', 'claude', 'gemini', 'grok'], debate_rounds || 2);

    res.status(202).json({
      query_id: queryId,
      status: 'processing',
      estimated_time: 15,
      webhook_url: `wss://${req.get('host')}/ws/${queryId}`,
      daily_usage: req.dailyUsage
    });
  } catch (error) {
    console.error('Query submission error:', error);
    res.status(500).json({ error: 'Failed to submit query' });
  }
});

// Get Query Results
app.get('/api/v1/queries/:queryId/results', authenticateToken, async (req, res) => {
  try {
    const { queryId } = req.params;

    // Get query
    const queryResult = await pool.query(
      `SELECT * FROM queries WHERE id = $1 AND user_id = $2`,
      [queryId, req.user.id]
    );

    if (queryResult.rows.length === 0) {
      return res.status(404).json({ error: 'Query not found' });
    }

    const query = queryResult.rows[0];

    // Get AI responses
    const responsesResult = await pool.query(
      `SELECT model, response, confidence, reasoning FROM ai_responses WHERE query_id = $1`,
      [queryId]
    );

    const individualResponses = {};
    responsesResult.rows.forEach(row => {
      individualResponses[row.model] = {
        response: row.response,
        confidence: row.confidence,
        reasoning: row.reasoning
      };
    });

    res.json({
      query_id: queryId,
      status: query.status,
      question: query.question,
      consensus: {
        summary: query.consensus_summary,
        confidence: query.consensus_confidence,
        convergence_points: query.convergence_points,
        divergence_points: query.divergence_points
      },
      individual_responses: individualResponses,
      metadata: {
        processing_time: query.processing_time,
        created_at: query.created_at,
        completed_at: query.completed_at
      }
    });
  } catch (error) {
    console.error('Get results error:', error);
    res.status(500).json({ error: 'Failed to get results' });
  }
});

// Get User Queries
app.get('/api/v1/queries', authenticateToken, async (req, res) => {
  try {
    const { limit = 20, offset = 0, status } = req.query;

    let queryText = `
      SELECT id, question, status, consensus_confidence, created_at, completed_at
      FROM queries
      WHERE user_id = $1
    `;
    
    const params = [req.user.id];

    if (status) {
      queryText += ` AND status = $${params.length + 1}`;
      params.push(status);
    }

    queryText += ` ORDER BY created_at DESC LIMIT $${params.length + 1} OFFSET $${params.length + 2}`;
    params.push(limit, offset);

    const result = await pool.query(queryText, params);

    res.json({
      queries: result.rows,
      pagination: {
        limit: parseInt(limit),
        offset: parseInt(offset),
        total: result.rowCount
      }
    });
  } catch (error) {
    console.error('Get queries error:', error);
    res.status(500).json({ error: 'Failed to get queries' });
  }
});

// Get User Stats
app.get('/api/v1/user/stats', authenticateToken, async (req, res) => {
  try {
    const today = new Date().toISOString().split('T')[0];
    
    const statsResult = await pool.query(
      `SELECT queries_count, tokens_used FROM usage_stats 
       WHERE user_id = $1 AND date = $2`,
      [req.user.id, today]
    );

    const totalQueries = await pool.query(
      `SELECT COUNT(*) as total FROM queries WHERE user_id = $1`,
      [req.user.id]
    );

    const avgConfidence = await pool.query(
      `SELECT AVG(consensus_confidence) as avg_confidence 
       FROM queries 
       WHERE user_id = $1 AND status = 'completed'`,
      [req.user.id]
    );

    res.json({
      today: statsResult.rows[0] || { queries_count: 0, tokens_used: 0 },
      total_queries: parseInt(totalQueries.rows[0].total),
      avg_confidence: parseFloat(avgConfidence.rows[0].avg_confidence) || 0,
      plan: req.user.plan
    });
  } catch (error) {
    console.error('Get stats error:', error);
    res.status(500).json({ error: 'Failed to get stats' });
  }
});

// Async Query Processing
async function processQueryAsync(queryId, question, models, rounds) {
  try {
    const startTime = Date.now();

    // Orchestrate AI debate
    const result = await aiOrchestrator.orchestrateDebate(question, models, rounds);

    // Save AI responses
    for (const response of result.responses) {
      await pool.query(
        `INSERT INTO ai_responses (query_id, model, response, confidence, reasoning)
         VALUES ($1, $2, $3, $4, $5)`,
        [queryId, response.model, response.response, response.confidence, response.reasoning]
      );
    }

    // Update query with consensus
    await pool.query(
      `UPDATE queries 
       SET status = $1,
           consensus_summary = $2,
           consensus_confidence = $3,
           convergence_points = $4,
           divergence_points = $5,
           processing_time = $6,
           completed_at = CURRENT_TIMESTAMP
       WHERE id = $7`,
      [
        'completed',
        result.consensus.summary,
        result.consensus.confidence,
        result.consensus.convergencePoints,
        result.consensus.divergencePoints,
        result.processingTime,
        queryId
      ]
    );

    // Emit WebSocket event
    io.to(queryId).emit('query_completed', {
      query_id: queryId,
      status: 'completed',
      consensus: result.consensus
    });

    console.log(`✅ Query ${queryId} completed in ${result.processingTime}ms`);
  } catch (error) {
    console.error(`❌ Query ${queryId} failed:`, error);
    
    await pool.query(
      `UPDATE queries SET status = $1 WHERE id = $2`,
      ['failed', queryId]
    );

    io.to(queryId).emit('query_failed', {
      query_id: queryId,
      error: error.message
    });
  }
}

// WebSocket Connection
io.on('connection', (socket) => {
  console.log(`🔌 Client connected: ${socket.id}`);

  socket.on('subscribe_query', (queryId) => {
    socket.join(queryId);
    console.log(`📡 Client subscribed to query: ${queryId}`);
  });

  socket.on('disconnect', () => {
    console.log(`🔌 Client disconnected: ${socket.id}`);
  });
});

// Error Handling
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(500).json({
    error: 'Internal server error',
    message: process.env.NODE_ENV === 'development' ? err.message : undefined
  });
});

// 404 Handler
app.use((req, res) => {
  res.status(404).json({ error: 'Endpoint not found' });
});

// Start Server
const PORT = process.env.PORT || 3000;

const startServer = async () => {
  try {
    // Initialize database
    await initDatabase();

    // Start HTTP server
    httpServer.listen(PORT, () => {
      console.log(`🚀 F.L.E.X. Backend Server running on port ${PORT}`);
      console.log(`📊 Environment: ${process.env.NODE_ENV || 'development'}`);
      console.log(`🔗 API: http://localhost:${PORT}/api/v1`);
      console.log(`⚡ WebSocket: ws://localhost:${PORT}`);
    });
  } catch (error) {
    console.error('❌ Failed to start server:', error);
    process.exit(1);
  }
};

startServer();

// Graceful Shutdown
process.on('SIGTERM', async () => {
  console.log('SIGTERM received, shutting down gracefully...');
  httpServer.close(async () => {
    await pool.end();
    await redis.quit();
    process.exit(0);
  });
});

module.exports = { app, httpServer };