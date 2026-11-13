# Commandes

Les arguments entre `< >` sont obligatoires, ceux entre `[ ]` sont optionnels.
Les ids d'objets (`<objet>`) sont ceux du jeu, par exemple `Rock_Stone` ; la
casse n'a pas d'importance et la touche Tab propose les articles du shop.

Les commandes joueur sont ouvertes à tous par défaut (voir `RequirePermissions`
dans le [README](../README.md#configuration)). Les commandes admin exigent
toujours la permission `hytale.shop.admin`.

## Joueurs

### Shop

| Commande | Effet |
| --- | --- |
| `/shop` | Ouvre la fenêtre du shop (voir [Interface](interface.md)). |
| `/shop list [page]` | Liste les articles par pages de 12, avec leurs prix et votre solde. |
| `/shop search <texte>` | Cherche les articles dont l'id contient tous les mots (`/shop search oak plank`). |
| `/shop buy <objet> [quantité]` | Achète `quantité` unités (1 par défaut). |
| `/shop sell <objet> [quantité]` | Vend `quantité` unités de votre inventaire (1 par défaut). |
| `/shop sellall <objet>` | Vend toutes les unités de cet objet que vous portez. |

Règles d'une transaction :

- La quantité va de 1 à `MaxTransactionQuantity` (1000 par défaut).
  `/shop sellall` vend au plus cette quantité.
- **Achat** : refusé si votre solde est insuffisant ou si votre inventaire n'a
  pas la place pour toute la quantité. Rien n'est débité dans ces cas.
- **Vente** : refusée si vous n'avez pas la quantité demandée. Rien n'est
  retiré dans ce cas.
- Un article avec un prix `-` dans `/shop list` ne peut pas être acheté (ou
  revendu) : le shop ne propose que l'autre sens.
- L'inventaire compté est la barre rapide, le stockage et le sac à dos.

Exemples :

```
/shop search marble
/shop buy Rock_Stone 64
/shop sell Soil_Dirt 32
/shop sellall Wood_Oak_Trunk
/shop list 3
```

### Hcoins

| Commande | Alias | Effet |
| --- | --- | --- |
| `/hcoin` | `/balance`, `/bal` | Affiche votre solde. |
| `/hcoin <joueur>` | `/balance <joueur>` | Affiche le solde d'un joueur connecté. |
| `/pay <joueur> <montant>` | | Envoie des Hcoins à un joueur connecté. Il reçoit un message. |
| `/hcointop` | `/baltop` | Classement des comptes les plus riches (10 par défaut). |

`/pay` refuse un montant nul ou négatif, un solde insuffisant et un virement à
soi-même.

Exemple :

```
/pay Steve 250
```

## Admins

### Soldes : `/eco`

| Commande | Effet |
| --- | --- |
| `/eco give <joueur> <montant>` | Ajoute des Hcoins au solde du joueur, qui est prévenu. |
| `/eco take <joueur> <montant>` | Retire des Hcoins, sans descendre sous 0. |
| `/eco set <joueur> <montant>` | Fixe le solde (0 accepté). |

Le joueur doit être connecté. Ces commandes fonctionnent aussi depuis la
console du serveur.

Exemples :

```
/eco give Steve 1000
/eco take Steve 50
/eco set Steve 0
```

### Catalogue : `/shopadmin`

| Commande | Effet |
| --- | --- |
| `/shopadmin set <objet> <prixAchat> <prixVente>` | Fixe les prix d'un article, ou ajoute un objet qui n'est pas un bloc. |
| `/shopadmin remove <objet>` | Retire l'article du shop (un bloc automatique reste retiré après redémarrage). |
| `/shopadmin reset <objet>` | Annule `set` et `remove` : le bloc revient à son prix automatique. |
| `/shopadmin reload` | Recharge `shop.json` après une modification à la main. |

- `prixAchat` : ce que le joueur paie par unité. `0` = le shop ne le vend pas.
- `prixVente` : ce que le joueur reçoit par unité. `0` = le shop ne le rachète
  pas.
- Les deux prix ne peuvent pas valoir `0` en même temps.
- `<objet>` de `set` doit exister dans le jeu ; la touche Tab propose tous les
  objets.
- Tous les blocs du jeu sont déjà dans le shop avec un prix automatique (voir
  le [README](../README.md#prix-automatiques)) : `set` sert surtout à ajuster
  un prix.
- Si `prixVente` dépasse `prixAchat`, la commande l'applique mais avertit : les
  joueurs pourraient acheter puis revendre à l'infini.
- Si `shop.json` est invalide, `reload` garde les articles actuels et bloque les
  modifications jusqu'à un `reload` réussi.

Exemples :

```
/shopadmin set Rock_Gem_Diamond 500 200
/shopadmin set Ingredient_Bar_Iron 40 10
/shopadmin set Ore_Iron_Stone 0 15
/shopadmin remove Soil_Gravel
/shopadmin reset Soil_Gravel
```

## Messages d'erreur

| Message | Cause |
| --- | --- |
| `'X' is not sold in the shop` | L'objet n'est pas dans le catalogue. |
| `The shop does not sell X` / `does not buy X` | Le prix correspondant vaut `0`. |
| `You need N Hcoin for this purchase` | Solde insuffisant. |
| `Your inventory does not have room` | Pas assez de place pour toute la quantité. |
| `You do not have enough X to sell` | Quantité demandée supérieure à ce que vous portez. |
| `You can trade at most N items at once` | Quantité au-dessus de `MaxTransactionQuantity`. |
| `... could not be saved. Check the server logs.` | Échec d'écriture disque ; rien n'a changé. |
