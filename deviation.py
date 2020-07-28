import pandas as pd;

data = pd.read_csv("deviationData.csv")
non_zero_data = data.loc[data["Max value deviation"] != 0.0]

print(data.describe())
#OUTPUT
#Max value deviation
#count          6830.000000
#mean            116.276184
#std              89.191793
#min               0.333333
#25%              41.333333
#50%              98.666667
#75%             171.583333
#max             499.333333

print()
print(non_zero_data.describe())

#OUTPUT
#Max value deviation
#count          6830.000000
#mean            116.276184
#std              89.191793
#min               0.333333
#25%              41.333333
#50%              98.666667
#75%             171.583333
#max             499.333333