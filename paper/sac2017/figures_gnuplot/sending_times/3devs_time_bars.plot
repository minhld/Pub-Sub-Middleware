set auto x
set xtics font "Verdana,8"
set ytics font "Verdana,9"
set ylabel "Distribution time (ms)"
set grid nopolar
set nologscale y
set yrange [0:2500]
#set xrange [0:15]
set style data histogram
set style histogram cluster gap 1
set style fill solid border -1
set boxwidth 1
set xtics rotate 90
#set xtic scale 0
set key left top
set xtics ("1K" 1, "10K" 2, "50K" 3, "0.1M" 4, "0.2M" 5, "0.3M" 6, "0.4M" 7, "0.5M" 8, "0.6M" 9, "0.7M" 10, "0.8M" 11, "0.9M" 12, "1M" 13, "1.2M" 14, "1.5M" 15) scale 0.0
plot 'D:\android\PbSbMid\paper\sac2017\figures_gnuplot\sending_times\3devs_sum.dat' using 2:xtic(1) ti col fc rgb "#666" fillstyle pattern 7, \
	'' u 3 ti col fc rgb "#666" fillstyle pattern 2, \
	'' u 4 ti col fc rgb "#666" fillstyle pattern 3