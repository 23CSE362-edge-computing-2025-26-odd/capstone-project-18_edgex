# ga_classifier_server.py
from flask import Flask, request, jsonify
import pandas as pd
import numpy as np
import random

app = Flask(name)

# --- Load dataset (error if not found) ---
try:
    df = pd.read_csv("priority_dataset.csv")
    X = df[['drowsiness', 'rain', 'speed', 'humidity']].values
    # Normalize speed for rules (assuming max speed is ~120)
    X[:, 2] = X[:, 2] / 120.0
    y = df['priority'].values
except FileNotFoundError:
    print("FATAL: dataset.csv not found! Please create it.")
    exit()

# --- GA Setup & Training (runs once at startup) ---
# (The GA code you provided goes here, with minor corrections for clarity)
classes = list(set(y))
class_to_idx = {c: i for i, c in enumerate(classes)}
idx_to_class = {i: c for c, i in class_to_idx.items()}
y_encoded = np.array([class_to_idx[label] for label in y])
POP_SIZE, N_GEN, MUT_RATE, N_RULES, N_FEATURES = 20, 50, 0.2, 3, X.shape[1]
def random_rule(): return [random.uniform(0,1) for _ in range(N_FEATURES)] + [random.choice(range(len(classes)))]
def random_chromosome(): return [random_rule() for _ in range(N_RULES)]
def apply_rule(rule, sample): return rule[-1] if all(sample[i] >= rule[i] for i in range(N_FEATURES)) else None
def predict_chromosome(chrom, sample):
    for rule in chrom:
        out = apply_rule(rule, sample)
        if out is not None: return out
    return random.choice(range(len(classes)))
def fitness(chrom): return sum(predict_chromosome(chrom, x) == t for x, t in zip(X, y_encoded)) / len(y_encoded)
def tournament(pop, k=3): return max(random.sample(pop, k), key=lambda c: fitness(c))
def crossover(p1, p2):
    point = random.randint(0, N_RULES - 1)
    return p1[:point] + p2[point:]
def mutate(chrom):
    if random.random() < MUT_RATE:
        rule, f = random.choice(chrom), random.randint(0, N_FEATURES - 1)
        rule[f] = random.uniform(0, 1)
    return chrom
population = [random_chromosome() for _ in range(POP_SIZE)]
for gen in range(N_GEN):
    population = [mutate(crossover(tournament(population), tournament(population))) for _ in range(POP_SIZE)]
best_chrom = max(population, key=lambda c: fitness(c))
print("--- GA Classifier Server ---")
print("Best rule set evolved with accuracy:", fitness(best_chrom))

# --- API Endpoint ---
@app.route("/predict_priority", methods=["POST"])
def predict_priority():
    try:
        data = request.json
        sample = np.array([
            data["drowsiness"], data["rain"],
            data["speed"] / 120.0, # Normalize speed
            data["humidity"]
        ])
        pred_idx = predict_chromosome(best_chrom, sample)
        priority = idx_to_class[pred_idx]
        return jsonify({"priority": priority})
    except Exception as e:
        return jsonify({"error": str(e)}), 400

# --- Run Server ---
if name == "main":
    app.run(host="0.0.0.0", port=5000)