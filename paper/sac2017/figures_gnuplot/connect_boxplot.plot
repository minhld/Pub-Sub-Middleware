set style fill solid 0.25 border -1
set style boxplot outliers pointtype 7
set style data boxplot
set boxwidth  0.3
set grid nopolar
unset key
set border 2
set xtics nomirror
set ytics nomirror
set ylabel "Time consumed (ms)" 
set xtics font "Verdana,9"
set ytics font "Verdana,9"
set yrange [0:6500]
#set xtics rotate 90
set xtics ("G4" 1, "Asus ZF2" 2, "GS3" 3, "BLU" 4, "Moto-G4" 5) scale 0.0
plot for [i=1:5] 'D:\android\PbSbMid\paper\sac2017\figures_gnuplot\connect_boxplot.dat' using (i):i ls 6
