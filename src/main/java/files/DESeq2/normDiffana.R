#!/usr/bin/env Rscript
###############################################################################
## Normalisation and Differential analysis using DESeq2 R package
## for RNA sequencing analysis
##
##  Version 1.6 (06/18/2014)
##
## Author : Xavier Bauquet
###############################################################################

# -----------------------------------------------------------------------------
# buildCountMatrix
# Create a matrix of reads count
#
# Input:
#   files : a vector of files names
#   sampleLabel : a vector of sample names
#   projectPath: path to the project directory
#
# Ouput:
#   countMatrix : a reads count matrix
#
# Original author : Vivien DESHAIES
# -----------------------------------------------------------------------------
buildCountMatrix <- function(files, sampleLabel, expHeader){

    # read first file and create countMatrix
    countMatrix <- read.table(files[1],header=expHeader,stringsAsFactors=F,
      quote="")
    colnames(countMatrix) <- c("id", "count1")

    # read and merge all remaining files with the first
    for(i in 2:length(files)){

        # read files
        exp <- read.table(files[i],header=expHeader,stringsAsFactors=F,quote="")

        # lowercase exp columns names
        colnames(exp) <- c("id", paste("count", i, sep=""))

        # merge file data to count matrix by id
        countMatrix <- merge(countMatrix, exp, by="id", suffixes="_")
    }

    # name rows
    rownames(countMatrix) <- countMatrix[,1]

    # delete first row containing row names
    countMatrix <- countMatrix[,-1]

    # name columns
    colnames(countMatrix) <- sampleLabel
    return(countMatrix)
}

###############################################################################
# -----------------------------------------------------------------------------
# saveRawCountMatrix
#
#   A specific treatment is needed for the raw count matrix because the object
#   counts(dds) does not include the names of the samples as column names
#
#   input: dds -> DESeq object
#          fileName -> character (name of the file to output)
#   output: rawCountMatrix -> file
#
# -----------------------------------------------------------------------------

saveRawCountMatrix <- function(dds,fileName){

	countMatrix <- counts(dds)
	countMatrix <- cbind(countMatrix, row.names(countMatrix))

	# Add the samples names as column names + add column Id
	colData <- colData(dds)
	colnames(countMatrix) <- c(colData$Name, "Id")

	# Put Ids on the first column
	countMatrix <- countMatrix[, c("Id", colData$Name)]

    # Write the count matrix in a file
    write.table(countMatrix, paste(fileName, sep=""),sep="\t",row.names=F,
      col.names=T, quote=F)
}

###############################################################################
# -----------------------------------------------------------------------------
# saveCountMatrix
#
#   input: countMatrix -> data frame (count matrix)
#          fileName -> character (name of the file to output)
#   output: countMatrix -> file
# -----------------------------------------------------------------------------

saveCountMatrix <- function(countMatrix,fileName){

	# Add column Id
	countNames <- colnames(countMatrix)
	countMatrix <- cbind(countMatrix, row.names(countMatrix))
	colnames(countMatrix) <- c(countNames, "Id")

	# Put Ids on first column
	countMatrix <- countMatrix[,c("Id", countNames)]

    # Write the count matrix in a file
    write.table(countMatrix, paste(fileName, sep=""),sep="\t",row.names=F,
      quote=F)
}

###############################################################################
# -----------------------------------------------------------------------------
# printInformationStart
#
#   This function print several informations about the script at the begining:
#   the time, the R version used, the versions of packages used in the script
#   and the parameters
#
# -----------------------------------------------------------------------------
printInformationStart <- function(args){

	cat("\n\n########################\n")
    cat("Session Info\n")
    cat("########################\n")

	info <- capture.output(sessionInfo())
    for(i in 1:length(info)) {
		cat(info[i])
		cat('\n')
    }

    cat("\n\n########################\n")
    cat("Params\n")
    cat("########################\n")

    cat(paste("Figures                                        =", as.character(normFigTest)))
    cat(paste("\nDifferential analysis                          =", as.character(diffanaTest)))
    cat(paste("\nFigures of the differential analysis           =", as.character(diffanaFigTest)))
    cat(paste("\nContrast matrix for differential analysis      =", as.character(contrastTest)))
    cat(paste("\nName of the design file                        =", designPath))
    cat(paste("\nDESeq2 modele                                  =", deseqModel))
    cat(paste("\nProject name                                   =", projectName))
    cat(paste("\nHeader on expression files                     =", as.character(expHeader)))
    cat(paste("\nSize factors type for size factors estimation  =", as.character(sizeFactorType)))
    cat(paste("\nFit type for dispersions estimation            =", as.character(fitType)))
    cat(paste("\nStatistic test                                 =", as.character(statisticTest)))
	cat(paste("\nPrefix                                         =", prefix))
    cat("\n\n########################\n\n")

}

###############################################################################
# -----------------------------------------------------------------------------
# wrapTitle
#
#    Add \n inside the title of figures when too long
#
#    input: title -> character (title to wrap)
#           width -> int (max width for the title)
#    output: finalTitle -> character (wraped title)
#
# -----------------------------------------------------------------------------
wrapTitle <- function(title, width){

    finalTitle <- paste(strwrap(title,width=width), sep="\n")
    return(finalTitle)
}

###############################################################################
# -----------------------------------------------------------------------------
# buildColorVector
#
#   This function aims to prepare a vector of colors, one for each unique
#   condition of the design
#
#   input: design -> data frame (the design file)
#   output: coLors -> vector (the vector of colors)
#
# -----------------------------------------------------------------------------
buildColorVector <- function(design){

    # for a 2 conditions analysis
    if(length(unique(design$Condition))== 2){

        uniqueColors <- c("#A6CEE3","#1F78B4")

        # selection of the good number of colors for the analysis
        test <- lapply(design$Condition ,
	  function(x){x == unique(design$Condition)})

        coLors <- c()
        for (result in test){
            coLors <- c(coLors, uniqueColors[result])
        }

    # for a 3-12 conditions analysis, using of a paired set of colors
    }else if(2 < length(unique(design$Condition)) &&
      length(unique(design$Condition))<= 12){

        # download of the "Paired" set of colors from the RColorBrewer library
        uniqueColors <- brewer.pal(length(unique(design$Condition)), "Paired")

        # selection of the good number of colors for the analysis
        test <- lapply(design$Condition ,
	  function(x){x == unique(design$Condition)})

        coLors <- c()
        for (result in test){
            coLors <- c(coLors, uniqueColors[result])
        }

    # for an analysis with more than 12 conditions
    }else{

        uniqueColors <- rainbow(length(unique(design$Condition)))

        # selection of the good number of colors for the analysis
        test <- lapply(design$Condition ,
	  function(x){x == unique(design$Condition)})

        coLors <- c()
        for (result in test){
            coLors <- c(coLors, uniqueColors[result])
        }
    }
    return(coLors)
}

###############################################################################
# -----------------------------------------------------------------------------
# firstPlots
#
#   This function create 3 plots for raw count matrix: unpooled clustering plot,
#   unpooled PCA plot and unpooled null counts barplot
#
#   input: projectName -> character (name of the project)
#          count_mat -> data frame (the count matrix)
#   output: 3 plots -> png
#
# -----------------------------------------------------------------------------
firstPlots <- function(projectName, count_mat){

    cat("      Fig 1 - Unpooled clustering\n")
    png(paste(prefix, projectName,"-normalisation_unpooled_clustering.png", sep=""),
      width=1000, height=600)
        # calculation of the dispersion
        dist.mat <- dist(t(count_mat))
        plot(hclust(dist.mat), main=paste("Unpooled cluster dendrogram - ",
	  projectName, sep=""), xlab="")
    dev.off()


    cat("      Fig 2 - Unpooled PCA\n")
    pcaCount <- PCA(t(count_mat), graph=FALSE)
    png(paste(prefix, projectName,"-normalisation_unpooled_PCA.png",sep=""),
      width=1000, height=600)

        par(mar=c(5,5,5,20))
        plot.PCA(pcaCount, choix="ind", col.ind=as.character(design$coLors),
	  title = paste("Unpooled PCA - ", projectName, sep=""))

        cor<-par('usr')
        par(xpd=NA)

        # add of legends
        legend(cor[2]*1.01,cor[4], title="Legend",
	  legend=unique(design$Condition),
	  col=unique(as.character(design$coLors)), pch=15, pt.cex=3, cex=1.2)

    dev.off()


    cat("      Fig 3 - Null counts barplot\n")
    png(paste(prefix, projectName,"-normalisation_null_counts.png",sep=""),width=1000,
      height=600)

        par(mar=c(15,8,5,20))
        barplot(100*colMeans(count_mat==0), cex.lab=2, las=3,
	  col=as.character(design$coLors) ,
	  main=paste("Proportion of null counts per sample -",
	    projectName, sep=" "),
	  ylab="Proportion of null counts (%)")

        cor<-par('usr')
        par(xpd=NA)

        # add of legends
        legend(cor[2]*1.01,cor[4], title="Legend",
	  legend=unique(design$Condition),
	  col=unique(as.character(design$coLors)), pch=15, pt.cex=3, cex=1.2)

    dev.off()
}

###############################################################################
# -----------------------------------------------------------------------------
# secondPlots
#
#   This function create 2 plots for raw count marix after deletion of
#   unexpressed genes and convertion of the count matrix and the design file
#   in a DESeq object: unpooled counts barplot, unpooled counts boxplot
#
#   input: projectName -> character (name of the project)
#          dds -> DESeq object
#   output: 2 plots -> png
#
# -----------------------------------------------------------------------------
secondPlots <- function(projectName, dds){

    cat("      Fig 4 - Unpooled counts barplot\n")
    png(paste(prefix, projectName,"-normalisation_barplot_counts.png", sep=""),
      width=1000, height=600)

        par(mar=c(15,8,5,20))
        barplot(colSums(counts(dds)),
	  main=paste("Read counts - ", projectName, sep=""),
	  col=as.character(colData(dds)$coLors),
	  names.arg =colData(dds)$Name,cex.lab=2, las=3,
	  ylab="Total read counts")

        cor<-par('usr')
        par(xpd=NA)

        # add legends
        legend(cor[2]*1.01,cor[4], title="Legend",
	  legend=unique(colData(dds)$Condition),
	  col=unique(as.character(colData(dds)$coLors)),
	  pch=15, pt.cex=3, cex=1.2)

    dev.off()


    cat("      Fig 5 - Unpooled counts boxplot\n")
    png(paste(prefix, projectName,"-normalisation_boxplot_count.png", sep=""),
      width=1000, height=600)

        par(mar=c(15,8,5,20))
        boxplot(log2(counts(dds)+1),
	  main=paste("Count distribution - ", projectName, sep=""),
	  col=as.character(colData(dds)$coLors),
	  names =colData(dds)$Name,cex.lab=2, las=3, ylab="log2 (counts+1)")

        cor<-par('usr')
        par(xpd=NA)

        # add legends
        legend(cor[2]*1.01,cor[4], title="Legend",
	  legend=unique(colData(dds)$Condition),
	  col=unique(as.character(colData(dds)$coLors)),
	  pch=15, pt.cex=3, cex=1.2)

    dev.off()
}

###############################################################################
# -----------------------------------------------------------------------------
# pooledPlots
#
#   This function create 2 plots for count matrix after collapsing of technical
#   replicates: pooled counts barplot and pooled counts boxplot
#
#   input: projectName -> character (name of the project)
#          dds -> DESeq object
#   output: 2 plots -> png
# -----------------------------------------------------------------------------
pooledPlots <- function(projectName, dds){

    cat("      Fig 6 - Pooled counts barplot\n")
    png(paste(prefix, projectName,"-normalisation_barplot_counts_pooled.png", sep=""),
      width=1000, height=600)

        par(mar=c(15,8,5,20))
        barplot(colSums(counts(dds)),
	  main=paste("Pooled read counts - ", projectName, sep=""),
	  col=as.character(colData(dds)$coLors),
	  names.arg =colData(dds)$RepTechGroup,cex.lab=2, las=3,
	  ylab="Total read counts")

        cor<-par('usr')
        par(xpd=NA)

        # add legends
        legend(cor[2]*1.01,cor[4], title="Legend",
	  legend=unique(colData(dds)$Condition),
	  col=unique(as.character(colData(dds)$coLors)),
	  pch=15, pt.cex=3, cex=1.2)

    dev.off()


    cat("      Fig 7 - Pooled counts boxplot\n")
    png(paste(prefix, projectName,"-normalisation_boxplot_count_pooled.png", sep=""),
      width=1000, height=600)

        par(mar=c(15,8,5,20))
        boxplot(log2(counts(dds)+1),
	  main=paste("Pooled count distribution - ", projectName, sep=""),
	  col=as.character(colData(dds)$coLors),
	  names =colData(dds)$RepTechGroup,cex.lab=2, las=3,
	  ylab="log2 (counts+1)")

        cor<-par('usr')
        par(xpd=NA)

        # add legends
        legend(cor[2]*1.01,cor[4], title="Legend",
	  legend=unique(colData(dds)$Condition),
	  col=unique(as.character(colData(dds)$coLors)),
	  pch=15, pt.cex=3, cex=1.2)

    dev.off()
}

###############################################################################
# -----------------------------------------------------------------------------
# normPlots
#
#   This function create 4 plots for count matrix after collapsing of technical
#   replicates and normalisation: pooled and normalised clustering, pooled and
#   normalised PCA, pooled and normalised boxplot and most expressed sequence
#   plot
#
#   input: projectName -> character (name of the project)
#          dds -> DESeq object
#   output: 2 plots -> png
#
# -----------------------------------------------------------------------------
normPlots <- function(projectName, dds){

    if(length(dds$RepTechGroup)>2){

        cat("      Fig 8 - Pooled and Normalised clustering\n")
        png(paste(prefix, projectName,"-normalisation_pooled_clustering.png", sep=""),
	  width=1000, height=600)

            # calculation of the dispertion
            ddsStabilized <- assay(varianceStabilizingTransformation(dds))
            dist.mat <- dist(t(ddsStabilized))
            plot(hclust(dist.mat),
	      main=paste("Pooled and Normalised cluster dendrogram - ",
		projectName, sep=""),
	      xlab="", labels=colData(dds)$RepTechGroup)

        dev.off()


        cat("      Fig 9 - Pooled and Normalised PCA\n")
        pcaCount <- PCA(t(counts(dds, normalized=TRUE)), graph=FALSE)
        png(paste(prefix, projectName,"-normalisation_normalised_PCA.png",sep=""),
	  width=1000, height=600)

            par(mar=c(5,5,5,20))
            plot.PCA(pcaCount, choix="ind",
	      col.ind=as.character(colData(dds)$coLors),
	      title = paste("Pooled and normalised PCA - ",
		projectName, sep=""))

            cor<-par('usr')
            par(xpd=NA)

            # add legends
            legend(cor[2]*1.01,cor[4], title="Legend",
	      legend=unique(colData(dds)$Condition),
	      col=unique(as.character(colData(dds)$coLors)),
	      pch=15, pt.cex=3, cex=1.2)

        dev.off()
    }
    else{
        cat("   Fig8: Pooled and Normalised clustering and Fig9: Pooled and Normalised PCA were escaped because we have less or 2 technical replicates")
    }

    cat("      Fig 10 - Pooled and Normalised boxplot\n")
    png(paste(prefix, projectName,"-normalisation_normalised_boxplot_count.png",sep=""),
      width=1000, height=600)

        par(mar=c(15,8,5,20))
        boxplot(log2(counts(dds,normalized=TRUE)+1),
	  main=paste("Pooled and Normalised count distribution - ",
	    projectName, sep=""),
	  col=as.character(colData(dds)$coLors),
	  names =colData(dds)$RepTechGroup,cex.lab=2, las=3,
	  ylab="log2 (counts+1)")

        cor<-par('usr')
        par(xpd=NA)

        # add legends
        legend(cor[2]*1.01,cor[4], title="Legend",
	  legend=unique(colData(dds)$Condition),
	  col=unique(as.character(colData(dds)$coLors)),
	  pch=15, pt.cex=3, cex=1.2)

    dev.off()


    cat("      Fig 11 - Most expressed features plot\n")

    # preparation of 2 data frame with the same number of column than
    # the dds count matrix
    maxCounts <- counts(dds)[1,]
    transcriptNames <- counts(dds)[1,]

    # for each sample (column)
    for(i in 1:ncol(counts(dds))){

        # selection of the maximum number of count
        maxCounts[i] <- (max(counts(dds, normalized=TRUE)[,i])/sum(counts(dds,
	  normalized=TRUE)[,i]))*100

        # selection of the name of the features this the maximum of count
        transcriptNames[i] <- row.names(subset(counts(dds, normalized=TRUE),
	  counts(dds, normalized=TRUE)[,i]==
	    max(counts(dds, normalized=TRUE)[,i])))

    }

    png(paste(prefix, projectName,"-normalisation_most_expressed_features.png",sep=""),
      width=1000, height=600)

        par(mar=c(5,15,5,20))
        x <- barplot(maxCounts,
	  main=paste("Most expressed features - ", projectName, sep=""),
	  col=as.character(colData(dds)$coLors),horiz = TRUE,
	  names.arg =colData(dds)$RepTechGroup,las=1,cex.lab=2,
	  xlab="Proportion of reads (%)")

        # add names of the features on the plot bars
        text(0, x, labels= transcriptNames, srt=0, adj=0)

        cor<-par('usr')
        par(xpd=NA)

        # add legends
        legend(cor[2]*1.01,cor[4], title="Legend",
	  legend=unique(colData(dds)$Condition),
	  col=unique(as.character(colData(dds)$coLors)),
	  pch=15, pt.cex=3, cex=1.2)

    dev.off()
}

###############################################################################
# -----------------------------------------------------------------------------
# anadiff
#
#   This function perform the classical differential analysis without contrast
#   vector: comparison of 2 conditions from the Condition colomne of the design
#   file
#
#   input: dds -> DESeq object
#          condition1, condition2 -> vectors (names of the conditions to
#                                    compare)
#          param -> booleen (FALSE to escape plots)
#          projectName -> character (name of the project)
#   output: 4 plots -> png (using the anadiffPlots function)
#           diffana matrix of the comparison -> tsv (file containing results of
#               the differential analysis between both conditions)
#
# -----------------------------------------------------------------------------
anadiff <- function(dds, condition1, condition2, param, projectName){

    # selection of results of the comparison
    res <- results(dds, contrast=c("Condition", condition1, condition2))

    # function for plots
    if(param==TRUE)anadiffPlots(paste(condition1,"_vs_", condition2,sep=""),
      projectName,res)

    res <- as.data.frame(res)
    res <- data.frame(res, dispersions(dds))
    res <- res[order(res$padj),]

    saveCountMatrix(res,paste(prefix, projectName,"-diffana_",condition1,
      "_vs_",condition2,".tsv", sep=""))

    cat(paste("Comparison: ",paste(condition1,"_vs_", condition2,sep=""),
      " finish\n", sep=""))
}

###############################################################################
# -----------------------------------------------------------------------------
# contrastAnadiff
#
#   This function perform the differential analysis using contrast vector
#
#   input: dds -> DESeq object
#          nameContrastVec -> character (name of the contrast vector)
#          vsContrastVec -> character (comparison name of the contrast vector)
#          contrastVec -> vector (the contrast vector)
#          param -> booleen (FALSE to escape plots)
#          projectName -> character (name of the project)
#   output: 4 plots -> png (using the anadiffPlots function)
#           diffana matrix of the comparison -> tsv (file containing results of
#               the differential analysis between both conditions)
#
# -----------------------------------------------------------------------------
contrastAnadiff <- function(dds, nameContrastVec,vsContrastVec, contrastVec,
  param, projectName){

    # separation of the character string in a vector
    contrastVec <- unlist(strsplit(substr(contrastVec, 2,
      (nchar(contrastVec)-1)), ","))

    # selection of results with the contrast vector
    res <- results(dds, contrast= as.numeric(contrastVec))

    # change the % in the name in -
    vsContrastVec <- gsub("%","-",vsContrastVec)

    # function for plots
    #if(param==TRUE)anadiffPlots(nameContrastVec, projectName,res)

    res <- as.data.frame(res)
    res <- data.frame(res, dispersions(dds))
    res <- res[order(res$padj),]

    saveCountMatrix(res,
      paste(prefix, projectName,"-diffana_",nameContrastVec,".tsv", sep=""))

    cat(paste("Comparison: ",vsContrastVec," finish\n", sep=""))
}

###############################################################################
# -----------------------------------------------------------------------------
# anadiffPlots
#
#   This function create 4 plots for the both differential analysis function
#   (anadiff and contrastAnadiff): p-value plot, adjusted p-value plot, MA-plot
#   and differentially expressed gense according adjusted P-value plot
#
#   input: smallNames -> character (
#          condName -> character (name of the comparison or of the contrast
#                      vector)
#          projectName -> character (name of the project)
#          res -> DESeq object (results of the differential analysis)
#   output: 4 plots -> png
#
# -----------------------------------------------------------------------------
anadiffPlots <- function(condName, projectName,res){

    # Raw Pvalue plot
    png(paste(prefix, projectName,"-diffana_plot_pvalue_",condName,".png",sep=""),
      width=1000, height=600)

        title <- paste("Raw P-value plot ",condName," - ", projectName,sep="")
        hist(res$pvalue,main=wrapTitle(title,120), col="#1F78B4",
	  xlab="p-value")

    dev.off()


    # Adjusted Pvalue plot
    png(paste(prefix, projectName,"-diffana_plot_padj_",condName,".png",sep=""),
      width=1000, height=600)

        title <- paste("Adjusted P-value plot ",condName," - ",
	  projectName,sep="")
        hist(res$padj,main=wrapTitle(title,120), col="#FF7F00",
	  xlab="Adjusted p-value")

    dev.off()


    # MA-plot
    png(paste(prefix, projectName,"-diffana_MA_plot_",condName,".png",sep=""),
      width=1000, height=600)
        title <- paste("MA-plot of ",condName," - ", projectName,sep="")
        plotMA(res, alpha=0.05, ylim=c(-10,10), main= wrapTitle(title,120))
        legend("bottomright",
	  legend=c("adjusted p-value < 0.05", "adjusted p-value >= 0.05"),
	  fill=c("red3","gray0"))
    dev.off()


    # Number of differentially expressed features according to padj
    # adjusted p-value to plot
    p <- c(0.000001, 0.00001, 0.0001, 0.001, 0.01, 0.05)
    value <- c(0,0,0,0,0,0)

    # calculation of the number of features differentially expressed for
    # each adjusted p-value
    for(i in 1:length(p)){
        value[i] <- nrow(subset(res,
			(res$padj < p[i] && res$log2FoldChange < -1) ||
				(res$padj < p[i] && res$log2FoldChange > 1)  ))
    }

    png(paste(prefix, projectName,"-diffana_plot_differentially_expressed_features_",
      condName,".png",sep=""),width=600, height=600)

        par(mar=c(8,8,5,5))
        title <- paste("Differentially expressed features
	  according adjusted P-value ",condName," - ", projectName,sep="")

        x <- barplot(value, main=wrapTitle(title,60), col="tan",
			names.arg =as.character(p),cex.lab=2,
			ylab="Number of features differentially expressed")

        # add of the number of features differentially expressed on the
        # plot bars
        text(x, 0, labels= value, srt=90, adj=0)
        cor<-par('usr')
        par(xpd=NA)

    dev.off()
}

###############################################################################
# -----------------------------------------------------------------------------
# printInformationEnd
#
#   This function print the time at the end of the script
#
# -----------------------------------------------------------------------------
printInformationEnd <- function(){
    cat("\n############\n")
    cat("End: ")
    cat(format(Sys.time(), "%Y-%m-%d %H:%M:%S"))
    cat("\nSuccessful end of the analysis\n")
    cat("\n############\n")
}

###############################################################################
#
## Main
#
###############################################################################
args <- commandArgs(TRUE)
# -----------------------------------------------------------------------------
# Parameters
normFigTest <- as.logical(toupper(args[1]))
diffanaTest <- as.logical(toupper(args[2]))
diffanaFigTest <- as.logical(toupper(args[3]))
contrastTest <- as.logical(toupper(args[4]))
designPath <- args[5]
deseqModel <- args[6]
projectName <- args[7]
expHeader <- as.logical(toupper(args[8]))
sizeFactorType <- args[9]
fitType <- args[10]
statisticTest <- args[11]
contrastFile <- args[12]
prefix <- args[13]



    cat("\n\n########################################################\n")
    cat("Start of the Normalisation and Differential analysis DESeq2 script version 1.6\n")
    cat("########################################################\n\n")

    cat("Start: ")
    cat(format(Sys.time(), "%Y-%m-%d %H:%M:%S\n\n"))

# -----------------------------------------------------------------------------

	cat("\n\n########################\n")
    cat("Package loading\n")
    cat("########################\n")
	library(DESeq2)
	library(RColorBrewer)
	library(FactoMineR)

# -----------------------------------------------------------------------------

			cat("\n")
            printInformationStart(args)

            cat("\n\n########################\n")
            cat("1 - Read design file\n")

# load design file
design <- read.table(designPath, sep="\t", header=T, dec=".",
	    stringsAsFactors=F)


        cat("2 - Creation of the color vector\n")
coLors <- buildColorVector(design)
# add colors to the design
design <- data.frame(design, coLors)

        cat("3 - Count matrix building\n")
# computing of expression files in one unique file
count_mat <- buildCountMatrix(design$expressionFile, design$Name, expHeader)

### plots: unpooled clustering plot, unpooled PCA plot and unpooled null
### counts barplot
if(normFigTest==TRUE)firstPlots(projectName, count_mat)
###

        cat("4 - DESeq2 object building\n")
# creation of the DESeq object including the count matrix and the design file
dds <- DESeqDataSetFromMatrix(countData=count_mat, colData=design,
	design=as.formula(deseqModel))

### plots: unpooled counts barplot, unpooled counts boxplot
if(normFigTest==TRUE)secondPlots(projectName, dds)
###

        cat("5 - Saving of rawCountMatrix\n")
saveRawCountMatrix(dds,
    paste(prefix, projectName,"-normalisation_rawCountMatrix.tsv", sep=""))

# -----------------------------------------------------------------------------
#
#   Collapsing technical replicates
#
# -----------------------------------------------------------------------------
        cat("6 - Collapsing of technical replicates\n")

dds <- collapseReplicates(dds, groupby=dds$RepTechGroup)

### plots: pooled counts barplot and pooled counts boxplot
if(normFigTest==TRUE)pooledPlots(projectName, dds)
###

        cat("7 - Saving of rawPooledCountMatrix\n")
saveCountMatrix(counts(dds),
    paste(prefix, projectName,"-normalisation_rawPooledCountMatrix.tsv", sep=""))

# -----------------------------------------------------------------------------
#
#   Normalisation
#
# -----------------------------------------------------------------------------
        cat("8 - Normalisation\n")

dds <- estimateSizeFactors(dds, type=sizeFactorType)

### plots: pooled and normalised clustering, pooled and normalised PCA,
### pooled and normalised boxplot and most expressed sequence plot
if(normFigTest==TRUE)normPlots(projectName, dds)
###

        cat("9 - Saving of normalisedCountMatrix\n")
saveCountMatrix(counts(dds,normalized=TRUE),
    paste(prefix, projectName,"-normalisation_normalisedCountMatrix.tsv", sep=""))

# -----------------------------------------------------------------------------
#
#   Differential analysis
#
# -----------------------------------------------------------------------------

if(diffanaTest==TRUE){
        cat("10 - Dispersion estimations\n")
    dds <- estimateDispersions(dds, fitType=fitType)

    if(diffanaFigTest==TRUE){
        cat("      Fig 12 - Dispersion plot\n")
        png(paste(prefix, projectName,"-diffana_plot_disp.png",sep=""),
	  width=1000, height=600
	)

            plotDispEsts(dds,
	      main=paste("Dispersion estimation scatter plot - ",
	      projectName, sep="")
	    )

        dev.off()
    }

    # if differential analysis using contrast matrix
    if(contrastTest == TRUE){
        cat("11 - Differential analysis using contrast matrix\n")
        # statistical analysis
        dds <- DESeq(dds, test=statisticTest, betaPrior=TRUE,
		      modelMatrixType="expanded", fitType=fitType
		    )

        contrastMatrix <- read.table(contrastFile, sep="\t",
				      header=T, dec=".", stringsAsFactors=F
				    )

	# run the differential analysis with the contrast matrix
        for(i in 1:nrow(contrastMatrix)){
            contrastAnadiff(dds,contrastMatrix[i,1],
			      contrastMatrix[i,2],contrastMatrix[i,3],
			      diffanaFigTest, projectName
			   )
        }

    }else{
		cat("11 - Differential analysis without contrast matrix\n")

        # statistical analysis
        dds <- DESeq(dds, test=statisticTest, betaPrior=FALSE, fitType=fitType)


	# cast reference conditions as numeric
	design$Reference <- as.numeric(toupper(design$Reference))

		# unique list of conditions
        unique_condition <- unique(sort(design$Condition))

	# create matrix with Condition and Reference numeric value
	n <- numeric(length(unique_condition))
	matCondition <- data.frame(Condition=unique_condition, Reference=n)
	# Keep the max reference numeric value for each condition
	for(k in 1:nrow(matCondition)){
		c <- design[design$Condition==matCondition$Condition[k],]
		matCondition[k,2] <- max(c$Reference)
	}

		# remove condition with reference negative that have
		# to be ignore
		matCondition <- matCondition[matCondition$Reference >= 0,]
		# save the list of reference condition
		ref <- as.vector(matCondition[matCondition$Reference > 0,1])


        # if no reference condition
        if(length(ref)<1){
            cat("12 - Differential analysis without contrast matrix and without reference condition\n")

            for(i in 1:(length(unique_condition)-1)){
                for(j in (i+1):length(unique_condition)){
                    if(unique_condition[i] != unique_condition[j]){
                        anadiff(dds, unique_condition[j], unique_condition[i],
			  diffanaFigTest, projectName)
                    }
                }
            }
        # if reference condition
        }else{
            cat("12 - Differential analysis without contrast matrix and with reference condition\n")

			for(j in 1:length(ref)){
				for(i in 1:length(unique_condition)){

				conditionLine <- subset(matCondition, Condition ==
					unique_condition[i])

				refLine <- subset(matCondition, Condition == ref[j] )
				if(conditionLine$Reference > refLine$Reference |
					conditionLine$Reference == 0){

					anadiff(dds, unique_condition[i], ref[j],
					diffanaFigTest, projectName)
					}
				}
			}
		}
	}
}

# End printing
    printInformationEnd()
