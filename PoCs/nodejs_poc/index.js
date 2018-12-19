
function aplicarDesconto(valor, desconto){
   if(desconto > valor) return 0
   return valor - desconto
}
 
module.exports = {aplicarDesconto}