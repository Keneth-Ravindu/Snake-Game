from datetime import datetime
from typing import List

from fastapi import FastAPI
from pydantic import BaseModel
from sqlalchemy import create_engine, Column, Integer, String, DateTime
from sqlalchemy.orm import declarative_base, sessionmaker

DATABASE_URL = "sqlite:///./scores.db"

engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False}
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)
Base = declarative_base()

app = FastAPI(title="Neon Serpent Backend")


class Score(Base):
    __tablename__ = "scores"

    id = Column(Integer, primary_key=True, index=True)
    player_name = Column(String, default="Player")
    score = Column(Integer)
    level = Column(Integer)
    rival_score = Column(Integer)
    loss_reason = Column(String)
    best_move = Column(String)
    risk_level = Column(String)
    created_at = Column(DateTime, default=datetime.utcnow)


Base.metadata.create_all(bind=engine)


class ScoreCreate(BaseModel):
    player_name: str
    score: int
    level: int
    rival_score: int
    loss_reason: str
    best_move: str
    risk_level: str


class ScoreResponse(ScoreCreate):
    id: int
    created_at: datetime

    class Config:
        from_attributes = True


class AnalysisRequest(BaseModel):
    score: int
    level: int
    rival_score: int
    loss_reason: str
    best_move: str
    risk_level: str


@app.get("/")
def root():
    return {"message": "Neon Serpent Backend is running"}


@app.post("/scores", response_model=ScoreResponse)
def save_score(data: ScoreCreate):
    db = SessionLocal()

    new_score = Score(
        player_name=data.player_name,
        score=data.score,
        level=data.level,
        rival_score=data.rival_score,
        loss_reason=data.loss_reason,
        best_move=data.best_move,
        risk_level=data.risk_level,
    )

    db.add(new_score)
    db.commit()
    db.refresh(new_score)
    db.close()

    return new_score


@app.get("/scores/top", response_model=List[ScoreResponse])
def get_top_scores():
    db = SessionLocal()

    scores = (
        db.query(Score)
        .order_by(Score.score.desc(), Score.level.desc())
        .limit(10)
        .all()
    )

    db.close()
    return scores


@app.post("/analysis")
def analyze_performance(data: AnalysisRequest):
    if data.risk_level == "High":
        advice = "You were surrounded by danger. Try moving earlier and avoid tight corners."
    elif data.risk_level == "Medium":
        advice = "You had some safe options, but your chosen route was risky."
    else:
        advice = "You played safely, but one mistake ended the run."

    if data.rival_score > data.score:
        rival_comment = "The rival snake controlled the food better than you."
    elif data.rival_score == data.score:
        rival_comment = "You and the rival snake were evenly matched."
    else:
        rival_comment = "You performed better than the rival snake."

    return {
        "feedback": data.loss_reason,
        "best_move": data.best_move,
        "risk_level": data.risk_level,
        "advice": advice,
        "rival_comment": rival_comment
    }