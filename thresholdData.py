import pandas as pd 
from matplotlib import pyplot as plt

data = pd.read_csv("thresholdData.csv")
print(data.columns)
data = data.set_index("Threshold")
plt.bar(data.index, data["Games"]*17)
plt.title("Number of games played at each threshold")
plt.xlabel("Threshold")
plt.ylabel("Number of games")
plt.show()