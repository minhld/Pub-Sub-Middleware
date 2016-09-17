set style fill solid 0.25 border -1
set style boxplot outliers pointtype 7
set style data boxplot
set boxwidth  0.3
#set pointsize 0.8
set grid nopolar
unset key
set border 2
set xtics nomirror
set ytics nomirror
set ylabel "Sending time (ms)" 
set xtics font "Verdana,8"
set ytics font "Verdana,9"
set yrange [0:1200]
set xtics rotate 90
set xtics ("1K" 1, "10K" 2, "50K" 3, "0.1M" 4, "0.2M" 5, "0.3M" 6, "0.4M" 7, "0.5M" 8, "0.6M" 9, "0.7M" 10, "0.8M" 11, "0.9M" 12, "1M" 13, "1.2M" 14, "1.5M" 15) scale 0.0
set style line 1 lc rgb '#eee' pt 7
plot for [i=1:15] 'D:\android\PbSbMid\paper\sac2017\figures_gnuplot\sending_times\1dev_time.dat' using (i):i ls 8
