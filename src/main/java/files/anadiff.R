#
# Differential analysis R script
#
# Authors : Maria Bernard
#           Marie-agnès Dilies
#

##########################################################################
######################## functions #######################################
##########################################################################
#Sums of technical replicates expression, and normalization thanks to the scaling factor normalization 
# of Ander and Hubert (implemented in the R package DESeq
# input:
#	* count: expression tab (one colomne per sample, with may be several sample per condition; one raw per gene)
#	* cond : a vector of number indicating to which condition the colmnes are belonging to.
#	* condNames : the names of the condition (one number in the cond vector corresponds to one name in condNames)
# output:
#	* normdeseq: normalized expression tab (one colomne per condition, one raw per gene)
norm_ss_rep = function(count,cond,condNames)
{
  # for all conditions sums of technical replicates expression
  tab=c()
  for (i in 1:length(unique(cond)))
  {
    tab = cbind(tab,rowSums(count[which(cond == i)]))
  }

  colnames(tab) = condNames
  rownames(tab)=rownames(count)

  # deleting genes that are not expressed in any condition
  tab = tab[rowSums(tab)>0,]

  #normalization thanks to the scaling factor of Anders and Hubert
#   K <- tab[apply(tab, 1, min) > 0,]
#   m <- ncol(K)
#   gmean <- (apply(K, 1, prod))^(1/m)
#   s <- NULL
#   for (j in 1:m) s <- c( s, median(K[,j]/gmean) )
#   normdeseq <- round( tab / matrix( s, nrow=nrow(tab), ncol=ncol(tab), byrow=T ) )

  #return of normalization expression
#   return(normdeseq)
  return(tab)
}

# statistical analysis of differentially expressed genes using a fisher exact test between two conditions
# the p-values  are corrected thanks to the Benjamini&Hochberg methods
# log2 fold change are also calculated on normalized expression values
# input : 
#   * tabTest : normalized expression table (one colomne per condition, one raw per gene)
#   *cond1, cond2: names of condition 1 and 2
#   * out: the path to the ouput file, which will contain the result of condition 1 vs condition 2  
ana_diff_ssrep = function(tabTest,cond1,cond2,out)
{
  library(statmod)
  library(edgeR)

  tab = cbind(tabTest[,cond1],tabTest[,cond2])
  colnames(tab) = c(cond1,cond2)

  f1 <- calcNormFactors(as.matrix(tab))
  cs <- colSums(tab)
  sizeC1 = sum(tab[,cond1])/sqrt(f1[2])
  sizeC2 = sum(tab[,cond2])*sqrt(f1[2])
  # calculation of p-values with a fisher exact test
  pvaldeseq <- sage.test(tab[,cond1], tab[,cond2], n1=sizeC1, n2=sizeC2)
  # correction of p-value using Benjamini&Hochberg method
  p.adjdeseq = p.adjust(pvaldeseq,"BH")

  # calculation of the log fold change and addition the results table
  logFC = log2(as.numeric(tab[,cond1])) - log2(as.numeric(tab[,cond2]))
   
  # creation of the results table   
  tab = cbind(tab,pvaldeseq,p.adjdeseq,logFC)
  tab = tab[order(data.frame(tab)$p.adjdeseq),]

  tab = cbind(rownames(tab),tab)
  colnames(tab) = c("Id",paste(cond1,"norm",sep=''),paste(cond2,"norm",sep=''),"pval","padj","logFC")

  # writing the results table in the file "out"
   write.table(tab,out,sep='\t',row.names=F, quote=F)
}

# Sum of technical replicat, normalization and variance estimation using a the DESeq software (Negativ Binomial model)
# log2 fold change are also calculated on the mean expression in the condition 1 and 2 taking into account the new librairies sizes
# input : 
#   * count: expression tab (one colomne per sample, with may be several sample per condition; one raw per gene)
#   * cond : a vector of number indicating to which condition the colmnes are belonging to.
#   * rep : a vector of charactere "T" or "B" indicating to which type of replicat the colmnes are corresponding to.
# output:
#   * cds: a CountDataSet object containing the counts, normalized counts and new librairies sizes
sum_rept_norm_acrepbio = function(count,cond,rep)
{
  # For each condition, sum of technical replicates and concatenation of biological replicats in a new expression table, tab
  tab = c()
  if (length(rep[which(rep == 'T')])>0)
  {
    tab = cbind(rowSums(count[which(cond == 1 & rep == "T")]),count[which(cond == 1 & rep == "B")])
    cond2 = rep(1,dim(tab)[2])
    for (i in 2:length(unique(cond)))
    {
      tab = cbind(tab,rowSums(count[which(cond == i & rep == "T")]),count[which(cond == i & rep == "B")])
      # update of the cond vector by removing condition code of technical replicate
      cond2 = c(cond2,rep(i,dim(cbind(rowSums(count[which(cond == i & rep == "T")]),count[which(cond == i & rep == "B")]))[2]))
    }
    cond = cond2
  }  else     {tab = count}
  # deleting genes that are not expressed in any condition
  tab = tab[rowSums(tab)>0,]

  # CountDataSet object creation, normalization, and variance estimation
  cds = newCountDataSet(tab,cond)
  cds = estimateSizeFactors(cds)
  cds = estimateVarianceFunctions(cds)

  # return of the CountDataSet object, cds
  return(cds)
}

