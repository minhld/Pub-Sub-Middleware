set boxwidth 0.5 absolute
set style fill solid 1.00 border lt -1
set grid nopolar
set pointsize 1
set ylabel "Time consumed (ms)" 
set ytics font "Verdana,11"
set yrange [ 0 : 60 ] 
unset xtics
set style line 1 lc rgb '#9f9f9c' pt 7
set style line 2 lc rgb '#367e96' pt 7
set style line 3 lc rgb '#782F3B' pt 7
plot 'D:\android\PbSbMid\paper\sac2017\figures_gnuplot\discover.dat' using 1 with points ls 1 title "LG G4", \
'' using 2 with points ls 2 title "Asus ZF2", \
'' using 3 with points ls 3 title "Galaxy S3"
