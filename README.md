# Comunicações por Computador (Protocolos aplicacionais & DNS)
## CC-20-21

Este trabalho foi realizado no âmbito da disciplina de CC (2020/21) existente na Universidade do Minho. Ao longo da unidade curricular foram desenvolvidos 3 trabalhos distintos:

- [TP1](https://github.com/pVeloso19/CC-20-21/blob/main/TP1/CC-TP1-PL1-G8.pdf)

> Resolução de questões que permitem identificar os vários protocolos aplicacionais existentes, bem como as suas características (camada de transporte, eficiência na transferência, complexidade e segurança). Neste trabalho discutiu-se, ainda a influência das situações de perda ou duplicação de pacotes IP no desempenho global de aplicações fiáveis.

- [TP2](https://github.com/pVeloso19/CC-20-21/blob/main/TP2/CC-TP2-PL1-G08-Rel.pdf)

> Neste trabalho pretende-se desenvolver um protocolo aplicacional capaz de funcionar como intermediário no pedido de um ficheiro.
>
>Com a criação do gateway pretende-se que o download de um arquivo seja dividido entre os vários servidores conectados, diminuindo assim a carga de trabalho de cada servidor. Contudo, uma vez que os arquivos se encontram nos servidores, estando assim em diferentes máquinas, é necessário realizar vários pedidos, utilizando para tal efeito o UDP, uma vez que, não sendo orientado à conexão é mais rápido na transferência de arquivos.
>
>Para a troca de mensagens entre os servidores e o HttpGw desenvolveu-se um formato para as mensagens protocolares, que contém sete campos sendo eles referentes ao tipo da mensagem transportada, ao nome do arquivo que se pretende descarregar, ao conteúdo do arquivo e como as mensagens são transportadas via UDP existe também um campo que indica se os dados recebidos estão completos, ou se é necessário receber ainda mais mensagens.
>
>Ao longo deste relatório pretende-se, explicar o funcionamento da aplicação desenvolvida, bem como especificar em detalhe todos os campos existentes nas mensagens protocolares, para além da sua importância. **Na figura seguinte exemplifica-se o funcionamento da aplicação, para um pedido de inicio de conexão e para um pedido de um ficheiro.**

<picture>
  <img alt="Esquematização do protocolo aplicacional" src="https://media.discordapp.net/attachments/1002574979252105312/1002575038492450886/Imagem1.png?width=980&height=546">
</picture>

- [TP3](https://github.com/pVeloso19/CC-20-21/blob/main/TP3/relatorio/cc-dns-PL1-G08.pdf)

> No último trabalho foi realizado um estudo sobre o funcionamento do DNS. Para tal foram realizadas algumas interrogações aos servidores de DNS. Sendo que no final foi implementado o nosso próprio servidor de DNS. **Assim recorrendo ao core foi possível implementá-lo tal como se observa na figura seguinte:**

<picture>
  <img alt="Funcionamento do servidor DNS" src="https://cdn.discordapp.com/attachments/1002574979252105312/1002577322198695946/Captura_de_ecra_2022-07-29_150337.png?width=593&height=546">
</picture>
<picture>
  <img alt="Funcionamento do servidor DNS reverso" src="https://cdn.discordapp.com/attachments/1002574979252105312/1002577322630721596/Captura_de_ecra_2022-07-29_150407.png">
</picture>

###### Nota Final: 16.5/20
