mvn compile
cd target/classes
echo 'Node build and now running..'

for i in {1..10}
do
	xterm -hold -e java Node.NodeDriver localhost localhost &
	sleep 0.5
done

java Node.PerformanceEvaluator localhost localhost
