#!/usr/bin/env Rscript
###############################################################################
## R script for creating a Bioconductor SingleCellExperiment object and save
## it in a RDS file.
##
## Author : Laurent Jourdren
###############################################################################

# Get command line arguments
args <- commandArgs(TRUE)

if (length(args) < 2) {
	stop("Invalid number of arguments.")
}

# Print session info
sessionInfo()

# Print script arguments
cat("Script arguments: ")
cat(args)
cat("\n")

# Parse arguments
matrixFile <- args[1]
outputFile <- args[2]
cellAnnotationFile <- NULL
featureAnnotationFile <- NULL

if (length(args) > 2) {
    i <- 0
    if (as.logical(toupper(args[3])) && length(args) > 3) {
        cellAnnotationFile <- args[4]
        i <- i + 1
    }

    if (as.logical(toupper(args[4 + i])) && length(args) > (4 + i) ) {
	featureAnnotationFile <- args[5 + i]
    }
}

# Import SingleCellExperiment package
library(SingleCellExperiment)

# Load counts
values <- read.table(matrixFile, header=TRUE, row.names=1, check.names=FALSE, sep="\t")

# Load cell annotations
cells <- NULL
if (!is.null(cellAnnotationFile)) {
    cells <-read.delim(cellAnnotationFile, header=TRUE, row.names=1, check.names=FALSE, sep="\t")
}

# Load feature annotations
features <- NULL
if (!is.null(featureAnnotationFile)) {
    features <- read.delim(featureAnnotationFile, header=TRUE, row.names=1, check.names=FALSE, sep="\t")
}

# Create SingleCellExperiment object
if (is.null(cells) && is.null(features)) {
    sce <- SingleCellExperiment(assays = list(counts = as.matrix(values)))
} else if (!is.null(cells)) {
    sce <- SingleCellExperiment(assays = list(counts = as.matrix(values)), colData = data.frame(cells))
} else if (!is.null(features)) {
    sce <- SingleCellExperiment(assays = list(counts = as.matrix(values)), rowData = data.frame(features))
} else {
    sce <- SingleCellExperiment(assays = list(counts = as.matrix(values)), colData = data.frame(cells), rowData = data.frame(features))
}

# Create RDS file from SingleCellExperiment object
saveRDS(sce, outputFile)

