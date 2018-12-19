
const test = require('tape')
const index = require('../index')


test('Aplicar desconto', (t) => {
    t.assert(index.aplicarDesconto(10,5) === 5, "Descontou corretamente")
    t.end()  
})

test('Aplicar desconto grande', (t) => {
    t.assert(index.aplicarDesconto(5,10) === 0, "Descontou corretamente")
    t.end()
})