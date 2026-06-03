# 🐍 Neon Serpent

> A modern AI-enhanced Snake Game built with Java Swing and FastAPI.

Neon Serpent reimagines the classic Snake game with a modern neon-inspired interface, intelligent AI rival, dynamic food system, obstacle mechanics, and backend-powered performance analytics.

---

# 🎮 Features

## Core Gameplay

- Classic Snake gameplay mechanics
- Smooth keyboard controls
- Dynamic difficulty progression
- Level-based speed increases
- Score tracking system

## AI Rival Snake

- Competes with the player for food
- Uses pathfinding-based movement
- Avoids walls and obstacles
- Avoids poison food
- Attempts to maximize its own score
- Creates competitive gameplay

## Food System

| Food Type | Effect |
|------------|----------|
| 🔴 Normal Food | +1 Score |
| 🟡 Golden Food | +5 Score |
| 🟣 Purple Food | Clears obstacles |
| 🟢 Poison Food | Instant Game Over |

## Obstacles & Environment

- Dynamic obstacle generation
- Boundary wall collisions
- Increasing challenge as levels progress

## Modern UI

- Neon-inspired visual design
- Animated food effects
- Particle effects
- Custom HUD
- Pause overlay
- Modern Game Over screen
- Custom player name screen with transition animation

## FastAPI Backend Integration

- Save player scores
- Store game statistics
- AI performance analysis
- Gameplay feedback generation

---

# 🧠 AI Features

## Rival Snake AI

The rival snake continuously analyzes the game board and:

- Calculates safe movement paths
- Avoids collisions
- Prioritizes valuable food
- Avoids poison items
- Competes against the player in real time

## Performance Analytics

When a game ends, the backend evaluates:

- Loss reason
- Best possible move
- Risk level
- Rival performance

Example feedback:

```text
You lost because you turned into a wall.

Best Move: LEFT
Risk Level: High

Advice:
Try moving earlier and avoid tight corners.

The rival snake controlled food better than you.
```

---

# 🛠️ Tech Stack

## Frontend (Game)

- Java
- Java Swing
- Java AWT
- Java2D Graphics
- Java Timer
- Java KeyListener
- Java HttpClient

## Backend

- Python
- FastAPI
- SQLAlchemy
- SQLite
- Pydantic
- Uvicorn

---

# 📂 Project Structure

```text
SnakeGame
│
├── src
│   ├── App.java
│   └── SnakeGame.java
│
├── snake-backend
│   ├── main.py
│   ├── requirements.txt
│   └── scores.db
│
└── README.md
```

---

# 🚀 Running the Game

## Compile

```bash
javac App.java SnakeGame.java
```

## Run

```bash
java App
```

---

# 🚀 Running the Backend

Navigate to:

```bash
cd snake-backend
```

Create virtual environment:

```bash
python -m venv .venv
```

Activate:

```bash
.venv\Scripts\activate
```

Install dependencies:

```bash
pip install -r requirements.txt
pip install fastapi uvicorn sqlalchemy pydantic
```

Run FastAPI:

```bash
python -m uvicorn main:app --reload
```

Swagger UI:

```text
http://127.0.0.1:8000/docs
```

---

# 📡 API Endpoints

## Save Score

```http
POST /scores
```

Stores:

- Player Name
- Score
- Level
- Rival Score
- Loss Reason
- Best Move
- Risk Level

## Top Scores

```http
GET /scores/top
```

Returns the highest scores.

## Performance Analysis

```http
POST /analysis
```

Returns gameplay feedback and recommendations.

---

# 🎯 Controls

| Key | Action |
|-------|---------|
| ↑ ↓ ← → | Move Snake |
| P | Pause / Resume |
| Space | Restart Game |
| Enter | Start Game |

---

# 🏗️ Software Engineering Concepts Used

## Object-Oriented Programming (OOP)

Implemented using:

- Classes
- Encapsulation
- Abstraction
- Composition
- Inheritance principles

## Event-Driven Programming

Implemented through:

- KeyListener
- ActionListener
- Swing Timer Events

## Data Structures

- ArrayList
- Custom Tile Objects
- Particle Systems

## AI Decision Making

- Heuristic Path Selection
- Collision Avoidance
- Risk Assessment
- Dynamic Rival Behaviour

## Client-Server Architecture

- Java Client
- FastAPI Server
- REST API Communication
- JSON Data Exchange

---

# 📸 Screenshots

Add screenshots here:

```text
screenshots/
│
├── start-screen.png
├── gameplay.png
├── pause-screen.png
├── ai-rival.png
└── game-over-analysis.png
```

Example:

```markdown
![Start Screen](screenshots/start-screen.png)
![Gameplay](screenshots/gameplay.png)
![Game Over](screenshots/game-over-analysis.png)
```

---

# 🔮 Future Improvements

- Online leaderboard
- Multiplayer mode
- Sound effects & music
- A* pathfinding AI
- Difficulty selection
- Player skins
- Achievement system
- Cloud-hosted backend
- Global rankings

---

# 👨‍💻 Author

**Keneth Ravindu**

Computer Science Graduate  
Aspiring AI/ML Engineer & Full-Stack Developer

## Skills Demonstrated

- Java Development
- Game Development
- Backend Development
- REST APIs
- AI Logic Design
- Software Architecture
- Object-Oriented Programming
- Full-Stack Development

---

# 📜 License

This project is licensed under the MIT License.

Feel free to use, modify, and distribute this project.
