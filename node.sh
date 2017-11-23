mvn compile
cd target/classes
echo 'Node build and now running..'

for i in {1..10}
do
	xterm -hold -e java Node.NodeDriver localhost localhost &
done

java Node.NodeQuery localhost localhost
