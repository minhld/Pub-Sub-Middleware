#set auto x
unset xtics
set yrange [0:600]
#set xtics font "Verdana,11"
set ytics font "Verdana,11"
set grid nopolar
set style data histogram
set style histogram cluster gap 1
set style fill solid border -1
#set xtic scale 0
set ylabel "Total time (ms)"
plot 'D:\documents\papers\sac2017\figures_gnuplot\net_perf_js_native.dat' using 1:xtic(1) title "1K message" with linespoints ls 2 lw 2 ps 1 lc rgb "#666" , \
	'' u 2 title "10K" with linespoints ls 2 lw 2 ps 1 lc rgb "orange", \
	'' u 3 title "100K" with linespoints ls 2 lw 2 ps 1 lc rgb "green", \
	'' u 4 title "500K" with linespoints ls 2 lw 2 ps 1 lc rgb "grey", \
	'' u 5 title "1M" with linespoints ls 2 lw 2 ps 1 lc rgb "red"
