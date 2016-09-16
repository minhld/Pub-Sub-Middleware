set style fill solid 0.25 border -1
set style boxplot outliers pointtype 7
set style data boxplot
set boxwidth  0.5
#set pointsize 0.5
set grid nopolar
unset key
set border 2
set xtics nomirror
set ytics nomirror
set ylabel "Sending time (ms)" 
set xtics font "Verdana,8"
set ytics font "Verdana,10"
set yrange [0:1100]
set xtics rotate 90
set xtics ("1K" 1, "10K" 2, "50K" 3, "0.1M" 4, "0.2M" 5, "0.3M" 6, "0.4M" 7, "0.5M" 8, "0.6M" 9, "0.7M" 10, "0.8M" 11, "0.9M" 12, "1M" 13, "1.2M" 14, "1.5M" 15) scale 0.0
set style line 1 lc rgb '#9f9f9c' pt 7
plot 'D:\android\PbSbMid\paper\sac2017\figures_gnuplot\data_gps_perf.dat' using (1):1 ls 8, \
'' using (2):2 ls 8, \
'' using (3):3 ls 8, \
'' using (4):4 ls 8, \
'' using (5):5 ls 8, \
'' using (6):6 ls 8, \
'' using (7):7 ls 8, \
'' using (8):8 ls 8, \
'' using (9):9 ls 8, \
'' using (10):10 ls 8, \
'' using (11):11 ls 8, \
'' using (12):12 ls 8, \
'' using (13):13 ls 8, \
'' using (14):14 ls 8, \
'' using (15):15 ls 8