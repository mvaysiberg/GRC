import pandas as pd;

data = pd.read_csv("deviationData.csv")
non_zero_data = data.loc[data["Max value deviation"] != 0.0]

print(data.describe())
#OUTPUT
#Max value deviation
#count         73404.000000
#mean             11.559865
#std              16.887943
#min               0.000000
#25%               0.000000
#50%               0.000000
#75%              19.333333
#max             109.333333

print()
print(non_zero_data.describe())

#OUTPUT
#Max value deviation
#count         36229.000000
#mean             23.421578
#std              17.321453
#min               0.333333
#25%              10.000000
#50%              20.000000
#75%              33.333333
#max             109.333333