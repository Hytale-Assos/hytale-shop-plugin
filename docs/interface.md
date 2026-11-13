# Interface du shop

La commande `/shop` ouvre la fenêtre du shop. Elle se ferme avec Échap ou le
bouton retour.

```
+--------------------------------------------------------------------------+
|                               Hcoin Shop                                 |
+--------------------------------------------------------------------------+
| Balance: 1,250 Hcoin       [Search (ex: stone)] [Search]   QTY  [ 16  ]  |
| [All] [Rocks] [Soils] [Wood] [Plants] [Metal] [Cloth] [Deco]             |
| [Ores] [Furniture] [Other]                                               |
|                  Bought 16x Stone for 240 Hcoin.                          |
| +----------------------------------------------------------------------+ |
| | [icone] Stone                          [ Buy ] [ Sell ] [ Sell all ] | |
| |         Buy: 15 Hcoin   Sell: 3 Hcoin                                | |
| |         In inventory: 16   (Rock_Stone)                              | |
| +----------------------------------------------------------------------+ |
| |  ... 25 articles par page ...                                        | |
| +----------------------------------------------------------------------+ |
|            [ < Prev ]   Page 1 / 104  (2600 items)   [ Next > ]          |
+--------------------------------------------------------------------------+
```

## Zones

- **Balance** : votre solde de Hcoins, mis à jour après chaque action.
- **Recherche** : tapez un ou plusieurs mots puis cliquez **Search**. Seuls les
  articles dont l'id contient tous les mots restent (`oak plank` trouve
  `Wood_Oak_Planks`). La recherche porte sur l'id anglais affiché entre
  parenthèses sous chaque article, pas sur le nom traduit. Videz le champ puis
  **Search** pour tout réafficher.
- **Qty** : quantité utilisée par les boutons **Buy** et **Sell** de toutes les
  lignes. Elle vaut 1 à l'ouverture et garde la dernière valeur saisie.
- **Onglets** : filtrent par catégorie (Rocks, Soils, Wood, Plants, Metal,
  Cloth, Deco, Ores, Furniture, Other). L'onglet actif est grisé. **All**
  affiche tout. Onglet et recherche se combinent.
- **Ligne de statut** : résultat de la dernière action, en vert si elle a
  réussi, en rouge sinon (mêmes messages que les
  [commandes](commandes.md#messages-derreur)).
- **Liste des articles** : 25 articles par page, triés par id. Chaque ligne
  montre l'icône, le nom dans la langue du joueur, le prix d'achat et de
  revente par unité (`-` si ce sens est désactivé), la quantité que vous portez
  et l'id de l'objet.
- **Pages** : **< Prev** et **Next >** changent de page ; le compteur indique la
  page et le nombre d'articles correspondant à l'onglet et à la recherche.
  Changer d'onglet ou de recherche revient à la page 1.

## Boutons d'un article

| Bouton | Effet | Désactivé quand |
| --- | --- | --- |
| **Buy** | Achète la quantité saisie. | Le shop ne vend pas cet objet. |
| **Sell** | Vend la quantité saisie. | Le shop ne rachète pas cet objet, ou vous n'en avez pas. |
| **Sell all** | Vend tout ce que vous portez de cet objet (au plus `MaxTransactionQuantity`). | Mêmes cas que **Sell**. |

Les règles sont celles des commandes `/shop buy`, `/shop sell` et
`/shop sellall` : solde suffisant et place dans l'inventaire pour acheter,
quantité possédée pour vendre. Une transaction refusée ne débite ni ne retire
rien. Après chaque action la page reste la même.

Une quantité qui n'est pas un nombre entier positif est refusée et le champ
revient à 1.

## Performances

Seule la page affichée (25 lignes) est envoyée au jeu, quel que soit le nombre
d'articles du shop, et l'inventaire est lu une seule fois par affichage. C'est
ce qui garde l'ouverture et les clics rapides avec les ~2 600 blocs.

## Pour les admins

- Le contenu suit les blocs du jeu et `shop.json` (voir le
  [README](../README.md#prix-automatiques)). Une modification via `/shopadmin`
  apparaît à la prochaine ouverture ou après la prochaine action dans une
  fenêtre déjà ouverte.
- Un article dont l'id n'existe pas dans le jeu n'est pas affiché.
- La mise en page est dans
  `src/main/resources/Common/UI/Custom/Pages/HytaleShop/` (`ShopPage.ui` pour
  la fenêtre, `ShopItemRow.ui` pour une ligne), et le comportement dans
  `ui/ShopPage.java`. La taille de page est `ShopPage.PAGE_SIZE`.
