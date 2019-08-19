import os
file_dir = r"F:\a star"
i = 1
a = os.walk(file_dir)
b = None
for root,dirs,files in os.walk(file_dir):
    print(i)
    i+= 1
    print(files)
