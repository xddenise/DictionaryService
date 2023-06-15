This is a program that uses socket to interact with a dictionary server which returns definitions for words.

To run the program (in your terminal):
1. Navigate to project
2. `make all` to compiling the project
3. `make run` to start the program or `make rund` to start the program in debug mode
4. `make clean` to remove the jars in order to recompile

After starting the program, here are the available commands:
- `open [test.dict.org] [2628]`: to open a connection with the dictionary server on port 2628
- `dict`: to list all dictionaries
- `set [dictionary]`: to set a dictionary as your current dictiionary, so the only definition you see is from this dictionary
- `define [word]`: to define a word
- `match [word]`: to show all words from dictionaries that match with the provided word
- `prefixmatch [word]`: to show all words from dictionaries that has the provided word as prefix
- `close`: to close the connection
- `quit`: to exit the program
