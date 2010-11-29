java -cp . DataGen 200 data.txt &
sleep 2
java -cp . Simulator data.txt 10000 true &
sleep 5
java -cp . RandomPlayer localhost 10000 rnd1 200 7 &
java -cp . RandomPlayer localhost 10000 rnd2 200 7 &
java -cp . RandomPlayer localhost 10000 rnd1 200 7 &
java -cp . RandomPlayer localhost 10000 rnd2 200 7 &
java -cp . RandomPlayer localhost 10000 rnd1 200 7 &
java -cp . RandomPlayer localhost 10000 rnd2 200 7 &
java -cp . RandomPlayer localhost 10000 rnd1 200 7 &
java -cp . RandomPlayer localhost 10000 rnd2 200 7 &
