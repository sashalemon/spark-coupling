library(tidyverse)
library(ggthemes)

read_interp <- function() {
    to.read <- file("../native/sim/main/interp.dat","rb")
    nd <- readBin(to.read, n=1, what="integer")
    x <- readBin(to.read, n=nd, what="double")
    y <- readBin(to.read, n=nd, what="double")
    close(to.read)
    return(data.frame(x,y))
}

read_gsl_hist <- function() {
    gslh <- read_delim("../native/sim/main/hist.tab",delim=" ",col_names=c("xmin","xmax","ymin","ymax","count"))
    gslh %>% 
        ggplot() + 
        geom_raster(aes(xmin,ymin,fill=count),
                    hjust = 1,
                    vjust = 1) + 
        geom_line(data = read_interp(), 
                  aes(x,y),
                  alpha=0.7) +
        scale_fill_distiller(palette = 'PuOr') +
        theme_tufte()
}

run <- function(fname) {
    df <- read.csv(fname)
    tx <- function(v) {
        v * (100/nrow(df))
    }
    ty <- function(v) {
        v * (100/nrow(df))
    }
    #f <- 
    cbind(x=seq(0,nrow(df)-1)%>%tx,df) %>% 
        gather('y','value',-one_of('x')) %>% 
        mutate(y=(as.integer(substring(y,2))-1) %>% ty) %>%
        ggplot(aes(x,y)) + 
        geom_raster(aes(fill=value),hjust = 1,vjust = 1,interpolate=T) +
        geom_line(data = read_interp(), alpha=0.7) +
        scale_fill_distiller(palette = 'RdYlBu') +
        theme_tufte()
}
run("fullrun.csv")
run("iter23.csv")
        #geom_tile(aes(fill=value, width=100/nrow(df)), interpolate=T) +
        #geom_ribbon(data = read_interp(), aes(ymin=0,ymax=y),alpha=0.2,fill="#0072B2") +
