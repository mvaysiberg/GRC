import numpy as np
import pandas as pd
from matplotlib import pyplot as plt

data = pd.read_csv("roundData.csv")

rounds = data["Round"].to_numpy()
Deadwood = data["Deadwood"].to_numpy()

plt.scatter(rounds,Deadwood)
plt.ylabel("Deadwood")
plt.xlabel("Round #")
plt.title("Deadwood per Round")

averages = data.groupby("Round")["Deadwood"].mean().to_numpy()
plt.scatter(np.unique(rounds),averages)
plt.show()
