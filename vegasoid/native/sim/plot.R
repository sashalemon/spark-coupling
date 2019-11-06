library('ggplot2')
library('ggthemes')
library('dplyr')
read_darts <- function() {
    to.read <- file("main/darts.dat","rb")
    nd <- readBin(to.read, n=1, what="integer")
    x <- readBin(to.read, n=nd, what="double")
    y <- readBin(to.read, n=nd, what="double")
    hit <- readBin(to.read, n=nd, what="integer")
    close(to.read)
    return(data.frame(x,y,hit))
}
read_interp <- function() {
    to.read <- file("main/interp.dat","rb")
    nd <- readBin(to.read, n=1, what="integer")
    x <- readBin(to.read, n=nd, what="double")
    y <- readBin(to.read, n=nd, what="double")
    close(to.read)
    return(data.frame(x,y))
}
run <- function() {
    darts <- read.table("main/c.tsv", header=T)
    darts %>% group_by(y) %>% summarize(sum(hit)) %>% arrange(desc(y)) %>% print()
    spline_interp <- read_interp()
    ggplot(spline_interp, aes(x,y)) +
    geom_ribbon(aes(ymin=-Inf, ymax=y), fill='#0072B2', alpha=0.5) +
    #geom_point(data = darts, aes(x,y,color=factor(hit),shape=factor(hit)), size=3) +
    geom_point(data = darts, aes(x,y,color=factor(hit)), size=1) +
    #geom_point(data = darts, aes(x,y,shape=factor(hit)), color='white') +
    geom_line() +
    scale_colour_colorblind() +
    theme_economist(dkpanel=T)
}
