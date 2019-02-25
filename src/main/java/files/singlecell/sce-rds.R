#!/usr/bin/env Rscript
###############################################################################
## R script for creating a Bioconductor SingleCellExperiment object and save
## it in a RDS file.
##
## Author : Laurent Jourdren
###############################################################################

# Get command line arguments
args <- commandArgs(TRUE)
matrixFile <- args[1]
cellAnnotationFile <- args[2]
geneAnnotationFile <- args[3]
outputFile <- args[4]

# Import SingleCellExperiment package
library(SingleCellExperiment)

# Load counts
values <- read.table(matrixFile, header=TRUE, row.names=1, check.names=FALSE, sep="\t")

# Load cell annotations
cells <-read.delim(cellAnnotationFile, header=TRUE, row.names=1, check.names=FALSE, sep="\t")

# Load gene annotations
genes <- read.delim(geneAnnotationFile, header=TRUE, row.names=1, check.names=FALSE, sep="\t")

# Create SingleCellExperiment object
sce <- SingleCellExperiment(assays = list(counts = as.matrix(values)), colData = data.frame(cells))

# Create RDS file from SingleCellExperiment object
saveRDS(sce, outputFile)

# Print session info
sessionInfo()
