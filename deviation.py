import pandas as pd;

data = pd.read_csv("deviationData.csv")
non_zero_data = data.loc[data["Max value deviation"] != 0.0]

print(data.describe())
#OUTPUT
#Max value deviation
#count          7477.000000
#mean            112.627792
#std              90.479094
#min               0.000000
#25%              38.000000
#50%              94.000000
#75%             169.333333
#max             554.666667

print()
print(non_zero_data.describe())

#OUTPUT
#Max value deviation
#count          7144.000000
#mean            117.877660
#std              89.158191
#min               0.333333
#25%              44.000000
#50%             100.000000
#75%             174.416667
#max             554.666667