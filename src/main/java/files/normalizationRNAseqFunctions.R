library(DESeq)


# -----------------------------------------------------------------------------
# dispersionEstimation
# perform estimation of dispersion
#
# Input :
#	cds1 : a countDataSet with estimated size factors
# 	replicates :  a boolean that indicates whether there is replicates or not
#				default = FALSE
#
# Output :
#	cds2 : a countDataSet with estimated dispersion
# -----------------------------------------------------------------------------
dispersionEstimation <- function(cds1, replicates=FALSE){
	
	if (replicates){
		cds2 <- estimateDispersions(
				cds1,
				sharingMode = "fit-only",
				fitType = "local"
		)
	}else{
		cds2 <- estimateDispersions(
				cds1,
				sharingMode = "fit-only", 
				fitType = "local",
				method = "blind"
		)
	}
	
	return (cds2)
}

# -----------------------------------------------------------------------------
# plotDispEsts
# function from S.Anders 2010
# plot  a doubly logarithmic representation of empirical (dots)  and fited 
# dispersion (red line)
# 
# Input : 
# 	cds :  a countDataSet with estimated dispersion
# -----------------------------------------------------------------------------
plotDispEsts <- function( cds ) {
	
	readCount <- counts(cds, normalized = TRUE)
	
	plot(
			rowMeans( readCount ), 
			fitInfo(cds)$perGeneDispEsts, pch = '.', log="xy",
			main = "Dispersion estimation scatter plot",
			xlab="log gene counts mean",
			ylab="log dispersion"
	)
	xg <- 10^seq( -.5, 5, length.out=300 )
	lines( xg, fitInfo(cds)$dispFun( xg ), col="red" )
	legend("bottomright", "fitted dispersion line", lwd=1, col="red")
	
}

# -----------------------------------------------------------------------------
# anaDiffDESeq
# Perform differential analysis
#
# Input :
# 	cds : a countDataSet object
# 	outpath : path where to save files
# -----------------------------------------------------------------------------
anaDiff <- function(cds, outpath){
	
	cond <- levels(conditions(cds))
	
	for(i in 1:(length(cond) - 1)){
		for(j in (i + 1):length(cond)){
			cond1 <- cond[i]
			cond2 <- cond[j]
			
			if(cond1 != cond2){
				# compute différential analysis
				result <- nbinomTest(cds, cond1, cond2)
				# rename columns
				colnames(result)[3] <- paste("baseMean", cond1, sep="_")
				colnames(result)[4] <- paste("baseMean", cond2, sep="_")
				colnames(result)[5] <- paste("FoldChange_", cond2,"-", cond1, sep="")
				# sort results by padj
				sortedResult<- result[order(result$padj),]
				# write results into a file
				nameComp <- paste(cond1, cond2, sep="-")
				write.table(
						sortedResult, 
						paste(outpath, "diffana_", nameComp, ".tsv", sep=""),
						sep="\t",row.names=F, quote=F
				)
				
				# plot MA-plot of the differential analysis
				maPlot(result, nameComp, outpath, out = TRUE)
				# plot pvalue distribution
				plotPvalueDist(result, cond2, cond1, outpath, out = TRUE)
			}else{}
		}
	}	
	
}

# -----------------------------------------------------------------------------
# anaDiffDESeqCinetic
# Perform differential analysis
#
# Input :
# 	cds : a countDataSet object
# 	outpath : path where to save files
# -----------------------------------------------------------------------------
anaDiffDESeqCinetic <- function(cds, ref, outpath){
	
	Conds <- levels(conditions(cds))
	
	for (cond in Conds) {
		if(cond != ref){
			# compute differential analysis
			result <- nbinomTest(cds, ref, cond)
			# rename columns
			colnames(result)[3] <- paste("baseMean", ref, sep="_")
			colnames(result)[4] <- paste("baseMean", cond, sep="_")
			colnames(result)[5] <- paste("FoldChange_", cond,"-", ref, sep="")
			# write results into a file
			nameComp <- paste(cond, ref, sep="-")
			write.table(
					result, 
					paste(outpath, "diffana_", nameComp, ".tsv", sep=""),
					sep="\t",row.names=F, quote=F
			)
			
			# plot MA-plot of the differential analysis
			maPlot(result, nameComp, outpath, out = TRUE)
			# plot pvalue distribution
			plotPvalueDist(result, cond, ref, outpath, out = TRUE)
		}else{}
	}
}

# -----------------------------------------------------------------------------
# maPlot
# Plot a MA-plot (use by anaDiff function)
#
# Input :
#	res : a result from nbinomTest function
# 	compName : string : the name of the comparison
# 	outpath :  path where to save the plot
# 	out (default = FALSE) : if TRUE plot is saved into a file 
# -----------------------------------------------------------------------------
maPlot <- function(res, compName, outpath = "", pvalThreshold = 0.05, out = FALSE){
	
	if (out){
		png(paste(outpath, "MA-plot-", compName, ".png", sep=""),
				width=700, height=600
		)
	}
	
	plot(
			res$baseMean[is.finite(res$log2FoldChange)],
			res$log2FoldChange[is.finite(res$log2FoldChange)],
			log="x", pch=20, cex=.3,
			# conditional coloration
			col = ifelse( 
					res$padj[is.finite(res$log2FoldChange)] < pvalThreshold,
					"red",
					"black"
			),
			main = paste("MA-plot", compName, sep=" "),
			xlab = "normalized mean",
			ylab = "log2 fold change"
	)
	
	legend(
			"bottomright",
			c(
					paste("pvaladj < ", pvalThreshold, sep=""), 
					paste("pvaladj >= ", pvalThreshold, sep="")
			),
			fill = c("red", "black")	
	)
	
	if (out){ dev.off() }
}


# -----------------------------------------------------------------------------
# normDESeq
# Create countDataSet object and estimates sizes factors
#
# Input:
# 	countMatrix : a matrix of reads counts with pooled data for technical 
#		replicates
#	cond : 	a vector of condition index
#	
# Output :
#	cds : 	a countDataSet object (DESeq object) with unnormalized counts and
#		estimation of size factors
# -----------------------------------------------------------------------------


normDESeq <- function(countMatrix, cond){
	# control of input objects
	if(length(cond) != ncol(countMatrix)){
		stop("condIndex length must be equal to countMatrix columns number")
	}else{}
	# create countDataSet object
	cds <- newCountDataSet(countMatrix, cond)
	# estimate size factors use in normalization
	cds <- estimateSizeFactors(cds)
	return (cds)	
}

# -----------------------------------------------------------------------------
# getNormCount
# Calculate normalized counts from raw counts
#
# Input:
#	cds : 	a countDataSet with size factor
#
# Ouput :
#	normCount : a matrix of normalized counts
# -----------------------------------------------------------------------------

getNormCount <- function(cds){
	# creation of size factors vector
	sizeFactors <- sizeFactors(cds)
	# Normalization of counts (scale with DESeq size factors)
	normCount <- scale(counts(cds), center=F, scale=sizeFactors)
	return (normCount)
}


# -----------------------------------------------------------------------------
# buildCountMatrix
# Create a matrix of reads count
#
# Input:
#	files : a vector of files names
#	sampleLabel : a vector of sample names
#	projectPath: path to the project directory
#
# Ouput:
#	countMatrix : a reads count matrix
# -----------------------------------------------------------------------------
buildCountMatrix <- function(files, sampleLabel, projectPath){
	
	# read first file and create countMatrix
	countMatrix <- read.table(paste(projectPath, files[1], sep=""),
			header=T,
			stringsAsFactors=F,
			quote=""
	)[,c("Id","Count")]
	# lowercase countMatrix columns names
	colnames(countMatrix) <- tolower(colnames(countMatrix))
	
	# read and merge all remaining files with the first
	for(i in 2:length(files)){
		# read files
		exp <- read.table(paste(projectPath, files[i], sep=""),
				header=T,
				stringsAsFactors=F,
				quote=""
		)[,c("Id","Count")]
		# lowercase exp columns names
		colnames(exp) <- tolower(colnames(exp))
		# merge file data to count matrix by id
		countMatrix <- merge(countMatrix, exp, by="id", suffixes="") 
	}
	# name rows
	rownames(countMatrix) <- countMatrix[,1]
	# delete first row containing row names
	countMatrix <- countMatrix[,-1]
	
	# name columns
	colnames(countMatrix) <- sampleLabel
	return(countMatrix)
}

# -----------------------------------------------------------------------------
# poolTechRep
# 
# Input: 
#	target : a target list
#
# Output:
#	pooledCountMatrix: a read count matrix with pooled technical replicates
#
# Modifiée le 12 mars 2012 pour prendre en argument un target
# -----------------------------------------------------------------------------

poolTechRep <- function( target ){
	
	# create poolTarget, a target list
	pooledTarget <- list()
	for( techGroup in unique( target$repTechGroup ) ){
		# bind pooled counts columns
		pooledTarget$counts <- cbind(
				pooledTarget$counts,
				# pool reads counts by technical replicates group
				rowSums(target$counts[which(target$repTechGroup == techGroup)] )
		)
		# concatenate corresponding condition in pooledTarget list
		pooledTarget$condition <- c(pooledTarget$condition,
				target$condition[which( target$repTechGroup == techGroup )[1]]
		)
		# concatenate corresponding sampleLabel in pooledTarget list
		pooledTarget$sampleLabel <- c(pooledTarget$sampleLabel, 
				target$sampleLabel[which( target$repTechGroup == techGroup )[1]]
		)
	}
	# input new pooled project name into pooledTarget list
	pooledTarget$projectName <- paste("pooled", target$projectName, sep="")
	
	# name pooled columns
	colnames(pooledTarget$counts) <- unique( target$repTechGroup )
	
	return( pooledTarget )
}

# -----------------------------------------------------------------------------
# deleteUnexpressedGene
# 
# Delete genes row whithout read count in a count matrix
# -----------------------------------------------------------------------------
deleteUnexpressedGene <- function(countMatrix){
	# Delete empty rows
	countMatrix <- countMatrix[rowSums(countMatrix)>0,]
	return(countMatrix)
}

# -----------------------------------------------------------------------------
# saveCountMatrix
# 
# Input: 
#	countMatrix : a read count matrix
#	outpath : the path where to save file
#	fileName : the file name
# -----------------------------------------------------------------------------

saveCountMatrix <- function(countMatrix, outpath, fileName){
	# Add ID column to make the file openable by calc or excel
	i <- length(countMatrix[1,])
	countMatrix <- cbind(countMatrix, rep(0, length(countMatrix[,1])))
	while(i >= 1){
		countMatrix[,i+1] <- countMatrix[,i]
		colnames(countMatrix)[i+1] <- colnames(countMatrix)[i]
		i <- i-1
	}
	countMatrix[,1] <- rownames(countMatrix)
	colnames(countMatrix)[1] <- "Id"
	# Write the count matrix in a file
	write.table(countMatrix, paste(outpath, fileName, sep=""),
			sep="\t",row.names=F, quote=F
	)
}

# -----------------------------------------------------------------------------
# barplotTotalCount
#
# plot a barplot of reads counts
# 
# Input: 
#	target : a target list
#	outpath : path where to save file (only if out = TRUE)
#	out : logical, if TRUE plot into a file (default = FALSE)
# -----------------------------------------------------------------------------

barplotTotalCount <- function(target, outpath = "", out= FALSE){

	if(out) {
		# verify if '/' is not missing at the end of path
		pathChar <- strsplit(outpath, "")
		if(!(pathChar[[1]][length(pathChar[[1]])] == "/")
				){
			stop("path must finish by '/'")
		}
		
		# create plot file
		png(paste(outpath, target$projectName, "barplotTotalCount.png", sep=""),
				width=1000, height=600
		)
	}
		# set plot margins
		par(omd=c(0.01,0.85,0.15,0.95))

	
	# create color vector (by condition)
	coLors <- rainbow(length(unique(target$condition)))
	test <- lapply(target$condition ,
			function(x){x == unique(target$condition)}
	)
	bioGroupColors <- c()
	for (result in test){
		bioGroupColors <- c(bioGroupColors, coLors[result])
	}
	# plot total counts barplot
	barplot(colSums(target$counts),
			las=3, 
			col=bioGroupColors,
			ylab="total read counts",
			main = paste(target$projectName, " total counts", sep="") 
	)
	# create a vector of extreme coordinates of plot region (x1, x2, y1, y2)
	userCoordinates <- par('usr')
	# set plot clipping to device region
	par(xpd=NA)
	# print legend on the plot
	legend(
			userCoordinates[2]*1.01,
			userCoordinates[4],
			title = "Legend",
			as.character(unique(target$condition)),
			fill = unique(bioGroupColors),
	)
	if (out){
		# close device (file)
		dev.off()
	}
}


# -----------------------------------------------------------------------------
# boxplotCounts
# Plot a boxplot of log2(counts + 1) by sample
#
# Input :
#	target : a target list
#	outpath : path where to save the file (only if out = TRUE)
#	out : logical, if TRUE plot into a file (default = FALSE)
# -----------------------------------------------------------------------------

boxplotCounts <- function(target, outpath = "", out=FALSE){
	
	if (out) {
		# verify if '/' is not missing at the end of path
		pathChar <- strsplit(outpath, "")
		if(!(pathChar[[1]][length(pathChar[[1]])] == "/")
				){
			stop("path must finish by '/'")
		}
		
		# create file
		png(paste(outpath, target$projectName, "boxplotCount.png", sep=""),
				width=1000, height=600
		)
	}
	
	# set plot margins
	par(omd=c(0.01,0.85,0.1,0.99))
	# create color vector
	coLors <- rainbow(length(unique(target$condition)))
	test <- lapply(target$condition ,
			function(x){x == unique(target$condition)}
	)
	bioGroupColors <- c()
	for (result in test){
		bioGroupColors <- c(bioGroupColors, coLors[result])
	}
	# plot boxplot(log2(count+1))
	boxplot(
			log2(target$counts +1),
			col=bioGroupColors,
			# vertical x labels
			las=3,
			ylab= "log2(counts+1)",
			main= paste(target$projectName, "count distribution", sep=" ")
	)
	# create a vector of extreme coordinates of plot region (x1, x2, y1, y2)
	userCoordinates <- par('usr')
	# set plot clipping to device region
	par(xpd=NA)
	# print legend on the plot
	legend(
			userCoordinates[2]*1.01,
			userCoordinates[4],
			title = "Legend",
			as.character(unique(target$condition)),
			fill = unique(bioGroupColors),
	)
	if (out) {
		# close file
		dev.off()
	}
}

# -----------------------------------------------------------------------------
# barplotNull
# barplot representing null counts per sample
# 
# Input : 
# 	target : a target list
# 	outpath : path where to save file ( only if out = TRUE)
#	out : logical, if TRUE plot into a file (default = FALSE)
# -----------------------------------------------------------------------------
barplotNull <- function(target, outpath = "" , out=FALSE){
	
	if (out) {
		# verify if '/' is not missing at the end of path
		pathChar <- strsplit(outpath, "")
		if(!(pathChar[[1]][length(pathChar[[1]])] == "/")
				){
			stop("path must finish by '/'")
		}
		# create plot file
		png(paste(outpath, target$projectName, "barplotNull.png", sep=""),
				width=1000, height=600
		)
	}
	
	# create null proportion vector
	N <- apply(target$counts,
			# apply on columns
			2,
			# return a vector of null count by column
			function(x){sum(x == 0)}
	)/nrow(target$counts)
	# set plot margins
	par(omd=c(0.01,0.85,0.15,0.95))
	# create color vector
	coLors <- rainbow(length(unique(target$condition)))
	test <- lapply(target$condition ,
			function(x){x == unique(target$condition)}
	)
	bioGroupColors <- c()
	for (result in test){
		bioGroupColors <- c(bioGroupColors, coLors[result])
	}
	# plot the barplot
	barplot(N,
			col = bioGroupColors,
			main = paste(target$projectName, ", proportion of null counts per Sample",sep=""),
			ylab = "proportion of null count",
			ylim = c(0,1),
			las=3
	)
	# create a vector of extreme coordinates of plot region (x1, x2, y1, y2)
	userCoordinates <- par('usr')
	# set plot clipping to device region
	par(xpd=NA)
	# print legend on the plot
	legend(
			userCoordinates[2]*1.01,
			userCoordinates[4],
			title = "Legend",
			as.character(unique(target$condition)),
			fill = unique(bioGroupColors),
	)
	
	if (out) {
		# close device
		dev.off()
	}
}

# -----------------------------------------------------------------------------
# densityplotRNA
#
# plot density plots of each column (sample) of the count matrix
# 
# Input:
#	target : a target list
#	outpath : path where to save the file (only if out = TRUE)
#	out : logical, if TRUE plot into a file (default = FALSE)
#
# Output:
#	a dendity plot into a file named "projectNameDensityPlot.png"
#	
# -----------------------------------------------------------------------------
densityplotRNA <- function(target, outpath = "", out=FALSE){
	
	if (out) {
		# verify if '/' is not missing at the end of path
		pathChar <- strsplit(outpath, "")
		if(!(pathChar[[1]][length(pathChar[[1]])] == "/")
				){
			stop("path must finish by '/'")
		}
		
		#create plot file
		png(paste(outpath, target$projectName,"DensityPlot.png", sep=""),
				width=1000, height=600
		)
	}

	# set plot margins
	par(omd=c(0.01,0.85,0.01,0.95))

	# create colors vector
	coLors <- rainbow(length(unique(target$condition)))
	test <- lapply(target$condition ,
			function(x){x == unique(target$condition)}
	)
	bioGroupColors <- c()
	for (result in test){
		bioGroupColors <- c(bioGroupColors, coLors[result])
	}
	# plot density
	plot(density(log2(target$counts[,1]+1)), col=bioGroupColors[1],
			main=paste(target$projectName, "density plot", sep=" ")
	)
	bandWidth <- density(log2(target$counts[,1]+1))$bw
	for(i in 2:length(target$counts[1,])){
		par(new=T)
		plot(density(log2(target$counts[,i]+1), bw=bandWidth),
				col=bioGroupColors[i], axes=F, main = "", xlab = "",
				ylab = ""
		)
	}
	# create a vector of extreme coordinates of plot region (x1, x2, y1, y2)
	userCoordinates <- par('usr')
	# set plot clipping to device region
	par(xpd=NA)
	# print legend on the plot
	legend(
			userCoordinates[2]*1.01,
			userCoordinates[4],
			title = "Legend",
			as.character(unique(target$condition)),
			lty=1,
			col=unique(bioGroupColors),
	)
	if(out){
		# close device
		dev.off()
	}
}

# -----------------------------------------------------------------------------
# buildTarget
# create target list
#
# Input : 
#	sampleLabels : a character vector of sample labels
#	projectName : name of the project (for plots)
#	filesNames : a character vector of files names
#	repTechGroup : a character vector of technicals replicates groups
#	condition : a character vector of condition names
#	exp : a comparison vector
#
# Output :
#	target : a target list
#
# author : Vivien Deshaies
# created Feb 9th 2012
# -----------------------------------------------------------------------------
buildTarget <- function(sampleLabels, projectName, fileNames, projectPath, repTechGroup, condition, exp){
	# create empty list
	target <- list()
	
	# verify inputs length
	if(
			length(sampleLabels) != length(fileNames) ||
			length(sampleLabels) != length(repTechGroup) ||
			length(sampleLabels) != length(condition) 
#			|| length(sampleLabel) != length(exp)
			){
		stop("sampleNames, fileNames, condition, repTechGroup and exp vectors must have the same length")
	}
	
	target$sampleLabel <- as.character(sampleLabels)
	

	# include project name into the target list
	target$projectName <- as.character(projectName)
	
	# include raw count matrix into target list
	target$counts <- buildCountMatrix(fileNames, sampleLabels, projectPath)
	
	# include technical replicates group into the target list
	target$repTechGroup <- repTechGroup
	
	# include conditions into the target list
	target$condition <- condition
	
	# include condition to compare to
	target$exp <- exp
	
	return(target)
}

# -----------------------------------------------------------------------------
# repClust
# hierarchical clustering of replicates to see if there is a mix between condition
# using correlation as distance and Ward's method
#
# Input : 
# 	target :  a target list
# 	outpath : path of the saving directory (only if out=TRUE)
# 	out : logical : if TRUE, plot is save into a file (default = FALSE)
#
# Output :
# 	a tree of cluster
# -----------------------------------------------------------------------------

repClust <- function(target, outpath = "", out= FALSE ){

	if (out) { 
		png(paste(outpath, target$projectName,"ClusterDendrogram.png", sep=""),
				width=800, height=600
		)
	}
	
	# plot dendrogram of clusters agglomerates by ward method, with correlation
	# 	between replicates as distance
	plot(
			# process clustering
			hclust(
					as.dist(1 - cor(target$counts)/2),
					# ward method : minimum sum of squares
					method="ward"
			),
			main=paste(target$projectName, " cluster dendrogram", sep=""),
			xlab="",
			sub=""
	)

	if (out) { dev.off() }
}

# -----------------------------------------------------------------------------
# normalizeTarget
# normalize a target list
#
# Input :
#	target : a target list
#
# Ouput :
#	normTarget : a normalized target list
# -----------------------------------------------------------------------------

normalizeTarget <- function(target){
	# create countDataSet (DESeq object)
	countDataSet <- normDESeq(target$counts, 1:length(target$counts[1,]))
	# normalize count
	normCount <- getNormCount(countDataSet)
	# create normTarget
	normTarget <- target
	# put normalized counts into normTarget
	normTarget$counts <- normCount
	# change projectName
	normTarget$projectName <- paste("norm", target$projectName, sep="")
	return(normTarget)
}

# -----------------------------------------------------------------------------
# sortTarget
# sort all element of a target list by condition
# -----------------------------------------------------------------------------

sortTarget <- function(target){
	# create sortedTarget list
	sortedTarget <- list()
	# generate index order by condition (condition)
	orderIndex <- order(target$condition)
	# order all element of the target list
	sortedTarget$sampleLabel <- target$sampleLabel[orderIndex]
	sortedTarget$projectName <- target$projectName
	sortedTarget$counts <- target$counts[,orderIndex]
	sortedTarget$repTechGroup <- target$repTechGroup[orderIndex]
	sortedTarget$condition <- target$condition[orderIndex]
	
	sortedTarget$exp <- target$exp # 
			
	return(sortedTarget)
}

# -----------------------------------------------------------------------------
# plotPvalueDist
# plot a barplot of p-values
# -----------------------------------------------------------------------------

plotPvalueDist <- function(anadiffResult, cond1, cond2, outpath="",out=FALSE){
	if (out){
		# verify if '/' is not missing at the end of path
		pathChar <- strsplit(outpath, "")
		if(!(pathChar[[1]][length(pathChar[[1]])] == "/")
				){
			stop("path must finish by '/'")
		}
		
		# create plot file
		png(paste(
				outpath, target$projectName, "_", cond1, "-", cond2, 
				"PvalueDistribution.png", sep=""
			),
			width=1000, height=600
		)
	}
	
	# set plot margins
	par(omd=c(0.01,0.85,0.1,0.99),mfrow=c(1,2))
	
	# plot the p-value distribution
	hist(anadiffResult$pval,
			breaks=100,
			col="skyblue",
			border="slateblue",
			xla="p-value",
			main = paste(cond1, "-", cond2," unajusted p-value distribution", sep=""),
	)
	# plot padj distribution
	hist(anadiffResult$padj,
			breaks=100,
			col="skyblue",
			border="slateblue",
			xla="padj",
			main = paste(cond1, "-", cond2," padj distribution", sep=""),
	)
	
#	# create a vector of extreme coordinates of plot region (x1, x2, y1, y2)
#	userCoordinates <- par('usr')
#	# set plot clipping to device region
#	par(xpd=NA)
#	# print legend on the plot
#	legend(
#			userCoordinates[2]*1.01,
#			userCoordinates[4],
#			title = "Legend",
#			as.character(unique(target$condition)),
#			fill = unique(bioGroupColors),
#	)
	
	if (out) { dev.off() }
	
}
