#! /bin/bash
echo "Generating data.txt..."
java -cp . DataGen 200 data.txt
sleep 1
echo "Spawning server..."
gnome-terminal --geometry=100x30 --title="SERVER" -x java -cp . Simulator data.txt 10000 true
sleep 5
echo "Spawning random 1..."
gnome-terminal --geometry=100x30 --title="RANDOM 1" -x java -cp . RandomPlayer localhost 10000 random1 200 5
echo "Spawning random 2..."
gnome-terminal --geometry=100x30 --title="RANDOM 2" -x java -cp . RandomPlayer localhost 10000 random2 200 5
echo "Now hit play on the Portfolio Simulator window!"

