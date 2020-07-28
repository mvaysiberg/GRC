import pandas as pd 

data = pd.read_csv("roundData.csv",index_col=0)
round_one = data.loc[data["Round"] == 1,"Deadwood"]

print(round_one.describe())

#OUTPUT

#count    5163.000000
#mean       46.674608
#std        15.094789
#min         0.000000
#25%        36.000000
#50%        46.000000
#75%        58.000000
#max        85.000000
#Name: Deadwood, dtype: float64

num_rounds = data.groupby("Game")["Round"].max()
print(num_rounds.describe())

#OUTPUT

#count    5163.000000
#mean        6.748983
#std         2.700133
#min         1.000000
#25%         5.000000
#50%         7.000000
#75%         8.000000
#max        16.000000
#Name: Round, dtype: float64