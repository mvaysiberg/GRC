import numpy as np
import pandas as pd
from matplotlib import pyplot as plt
from scipy import stats

data = pd.read_csv("roundData.csv")

max_rounds = data["Round"].max()
games = data["Game"].max()

fill_data = {"Game": [], "Round": [], "Deadwood": []}

for i in range(1,games+1):
    current_rounds = data.loc[data["Game"] == i,"Round"].max()
    min_dwood = data.loc[(data["Game"] == i) & (data["Round"] == current_rounds),"Deadwood"].to_numpy()[0]
    if  min_dwood <= 10 and current_rounds < max_rounds:
        for j in range(current_rounds +1,max_rounds+1):
            fill_data["Game"].append(i)
            fill_data["Round"].append(j)
            fill_data["Deadwood"].append(min_dwood)
FD = pd.DataFrame(fill_data)
data = data.append(FD,ignore_index=True).sort_values(by=["Game","Round"])
print(data)

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