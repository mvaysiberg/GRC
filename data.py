import numpy as np
import pandas as pd
from matplotlib import pyplot as plt
from scipy import stats

data = pd.read_csv("roundData.csv")

rounds = data["Round"].to_numpy()
unique_rounds = np.unique(rounds)
Deadwood = data["Deadwood"].to_numpy()

plt.scatter(rounds,Deadwood)
plt.ylabel("Deadwood")
plt.xlabel("Round #")
plt.title("Deadwood per Round")

averages = data.groupby("Round")["Deadwood"].mean().to_numpy()
plt.scatter(unique_rounds,averages)

slope, intercept, rvalue, pvalue, stderr = stats.linregress(unique_rounds,averages)
plt.plot(unique_rounds,slope*unique_rounds + intercept, color="RED")
plt.show()

print("Num Sub-games played: %d" %(np.count_nonzero(rounds == 1)))
print("correlation: %f\npvalue: %f\nstderr: %f" %(rvalue,pvalue,stderr))