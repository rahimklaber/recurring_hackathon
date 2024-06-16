## Contract
Contract is deployed here https://stellar.expert/explorer/testnet/contract/CDHXNJXGVROYZYVGJ5Z6SW3MAJYNPBQMPB42LD76YXKWNKGOYM3OZGJP
```rust
pub struct MandateConfig{
    pub amount: i128,
    pub start : u64,
    pub last_charged: u64,
    pub charge_interval: u64,
    pub shopper: Address,
    pub merchant: Address,
    pub token_id: Address,
}

pub trait MandateContractTrait {
    fn create_mandate(env: Env, config: MandateConfig, untill: u32) -> u64;
    fn charge_mandate(env: Env, mandate_id: u64);
    fn revoke_mandate(env: Env, mandate_id: u64);
}
```
### Create_mandate
The shopper can call create_mandate to create a subscription and give the contract permission to spend their funds. The shopper can specify a charge_interval. The subscription can be charged every charge_interval seconds.

### charge_mandate
The merchant can call charge_mandate every charge_interval seconds to charge the subscription. The function checks that charge_interval time has passed since the last charge.

### revoke_mandate
revoke_mandate can be called by the shopper to cancell the subscription.

## Backend
The backend is a server written in kotlin that acts as a middleman between the merchant and contract, the shopper and contract.
1. The merchant can make a POST request to `/payment` with the below structure to create a payment.
```json
{
  "amount": "100000000",
  "asset": "native",
  "type": "first",
  "chargeInterval": "4s",
  "description" : "YOU NEED TO PAY"
}
```
2. the server responds with something like this
```json
{
  "paymentId": "2",
  "redirectUrl": "http://localhost:8080/confirmmandate?id=2"
}
```
3. The merchant redirects the shopper to the redirectUrl.
4. The shopper confirms and signs the subscription creation transaction ![you_need_to_pay](https://github.com/rahimklaber/recurring_hackathon/assets/21971137/5634fe86-2698-41dc-a50c-c20e5af00a23)
5. The server submits the transaction and the shopper is shown a success screen.
6. The server continiously checks if it should charge the subscription. If so It calls the `charge_mandate` contract function to charge the shopper for the subscription.

