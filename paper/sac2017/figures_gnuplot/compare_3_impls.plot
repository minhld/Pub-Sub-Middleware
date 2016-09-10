set auto x
set xtics font "Verdana,11"
set ytics font "Verdana,11"
set ylabel "Package size (bytes)"
set ylabel "Distribution time (ms)"
set grid nopolar
set yrange [0:450]
set style data histogram
set style histogram cluster gap 1
set style fill solid border -1
set boxwidth 0.9
set xtic scale 0
plot 'D:\documents\papers\sac2017\figures_gnuplot\compare_3_impls.dat' using 2:xtic(1) ti col fc rgb "#666" fillstyle pattern 3, \
	'' u 3 ti col fc rgb "#666" fillstyle pattern 5, \
	'' u 4 ti col fc rgb "#666" fillstyle pattern 1